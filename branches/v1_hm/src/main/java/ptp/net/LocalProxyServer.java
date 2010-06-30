package ptp.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

import ptp.Config;
import ptp.net.http.RequestHttpMessage;
import ptp.net.http.ResponseHttpMessage;
import ptp.net.mp.MethodProcesser;
import ptp.net.util.HttpUtil;

public class LocalProxyServer {

	private static Logger log = Logger.getLogger(LocalProxyServer.class);

	int localProxyPort;
	int localProxyTimeOut;

	boolean isStopped = false;

	ServerSocket localProxyServerSocket;

	public LocalProxyServer() {
		localProxyPort = Integer.parseInt(Config.getIns().getValue(
				"ptp.local.proxy.port", "8887"));
		localProxyTimeOut = 1000;
	}

	public void startService() throws Exception {
		try {
			localProxyServerSocket = new ServerSocket(localProxyPort);
		} catch (IOException e) {
			log.error("create local proxy server socket failed", e);
			throw e;
		}
		log.info("local proxy server started on port: " + localProxyPort);

		localProxyServerSocket.setSoTimeout(1000);

		while (!isStopped) {
			Socket browserSocket = null;
			try {
				browserSocket = localProxyServerSocket.accept();
			} catch (SocketTimeoutException ste) {
				continue;
			}
			if (browserSocket != null) {
				log.info("visit from browser: "
						+ browserSocket.getInetAddress().getHostAddress() + " "
						+ browserSocket.getPort());
				Thread localProxyProcessThread = new Thread(
						new LocalProxyProcessThread(browserSocket));
				localProxyProcessThread.start();
				log.debug("thread count: " + Thread.activeCount());
			}
		}
		localProxyServerSocket.close();
		log.info("stop local proxy server");
	}

	public void stopService() {
		isStopped = true;
	}

}

class LocalProxyProcessThread implements Runnable {
	private static Logger log = Logger.getLogger(LocalProxyProcessThread.class);

	Socket browserSocket;

	public LocalProxyProcessThread(Socket browserSocket) {
		this.browserSocket = browserSocket;
	}

	@Override
	public void run() {
		process();
		try {
			browserSocket.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void process() {
		InputStream inFromBrowser = null;
		OutputStream outToBrowser = null;
		try {
			inFromBrowser = browserSocket.getInputStream();
			outToBrowser = browserSocket.getOutputStream();
		} catch (IOException e) {
			log.error("failed to open stream on browser socket", e);
			return;
		}

		int retry = 0;
		int processTimes = 0;
		while (browserSocket.isConnected()) {
			int availableBytes = 0;
			try {
				availableBytes = inFromBrowser.available();
			} catch (IOException e) {
				log.error(e.getMessage());
				break;
			}
			if (availableBytes > 0) {
				RequestHttpMessage reqHM = new RequestHttpMessage();
				try {
					reqHM.read(inFromBrowser);
					MethodProcesser mp = new MethodProcesser(reqHM);
					ResponseHttpMessage resHM = mp.process();
					outToBrowser.write(resHM.getHeadBytes());
					outToBrowser.flush();
					
					FileInputStream resHMFis = new FileInputStream(resHM.getBodyDataFile());
					int bodyReadCount=0;
					int buff_size = Integer.parseInt(Config.getIns().getValue("ptp.local.buff.size", "10240"));
					byte[] bodyBuff = new byte[buff_size];
					while(true) {
						int readCount = resHMFis.read(bodyBuff, 0 , buff_size);
						if(readCount >= 0) {
							try {
								outToBrowser.write(bodyBuff, 0, readCount);
							} catch(SocketException e) {
								log.info("stop by browser");
								resHM.stopReadBody();
								break;
							}
							outToBrowser.flush();
							bodyReadCount += readCount;
						} else {
							if(resHM.isBodyReadEnd() && bodyReadCount==resHM.getBodyDataFile().length()) {
								break;
							} else if(!resHM.isBodyReadEnd()){
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						
					}
					resHMFis.close();
					
					reqHM.clear();
					resHM.clear();
				} catch (ProxyException e) {
					log.error(e.getMessage(), e);
					HttpUtil.writeErrorResponse(outToBrowser, e);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					break;
				}
				processTimes++;
			} else {
				if (retry > 0) {
					break;
				}
				try {
					log.info("local proxy thread wait");
					Thread.sleep(150);
					retry++;
				} catch (InterruptedException e) {
					log.info(e.getMessage());
					break;
				}
			}
		}

		try {
			inFromBrowser.close();
			outToBrowser.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		log.info(Thread.currentThread().getName() + " end, it proceed "
				+ processTimes + " requests from browser");
	}

}