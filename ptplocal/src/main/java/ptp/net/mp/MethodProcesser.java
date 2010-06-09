package ptp.net.mp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import ptp.Config;
import ptp.util.Base64Coder;
import ptp.util.ByteArrayUtil;
import ptp.util.HttpUtil;

public abstract class MethodProcesser {
	private static Logger log = Logger.getLogger(MethodProcesser.class);

	// private MethodProcesser(){}

	public static MethodProcesser getIns(byte[] head, int headLen,
			InputStream inFromBrowser, OutputStream outToBrowser) {
		String headString = ByteArrayUtil.toString(head, 0, headLen);
		String[] headers = headString.split("\\r\\n");
		if (headers[0].startsWith("GET"))
			return new GetMethodProcesser(headers, inFromBrowser, outToBrowser);
		else if (headers[0].startsWith("POST"))
			return new PostMethodProcesser(headers, inFromBrowser, outToBrowser);
		else
			return null;

	}

	void requestRemote(byte[] data, String destHost, int destPort,
			boolean isSSL, OutputStream outToBrowser) {
		String remotePhp = Config.getIns().getRemotePhp();
		log.info("remotePhp: " + remotePhp);

		String requestBase64String = new String(Base64Coder.encode(data, 0,
				data.length));
		String requestEncodedString = null;
		try {
			requestEncodedString = URLEncoder.encode(requestBase64String, "US-ASCII");
		} catch (UnsupportedEncodingException e2) {
		}

		byte key = (byte) ((Math.random() * (100)) + 1);

		byte[] postData = null;

		try {
			postData = ("request_data=" + requestEncodedString + "&dest_host="
					+ destHost + "&dest_port=" + destPort + "&is_ssl=" + isSSL
					+ "&key=" + key).getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		log.debug("request: "
				+ ByteArrayUtil.toString(postData, 0, postData.length));

		URL remotePhpUrl = null;
		try {
			remotePhpUrl = new URL(remotePhp);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpURLConnection remotePhpConn = null;
		try {
			remotePhpConn = (HttpURLConnection) remotePhpUrl
					.openConnection(Config.getIns().getProxy());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			remotePhpConn.setRequestMethod("POST");
		} catch (ProtocolException e) {

		}
		remotePhpConn.setRequestProperty("User-Agent", Config.getIns()
				.getUserAgent());
		remotePhpConn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		remotePhpConn.setRequestProperty("Connection", "close");
		remotePhpConn.setUseCaches(false);
		remotePhpConn.setDoOutput(true);

		try {
			OutputStream outToPhp = remotePhpConn.getOutputStream();

			outToPhp.write(postData);

			outToPhp.flush();
			outToPhp.close();
		} catch (IOException e) {
			// TODO
		}

		InputStream inFromPhp = null;
		try {
			inFromPhp = remotePhpConn.getInputStream();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			System.exit(1);
		}
		try {

			int responseHeadReadCount = -1;
			byte[] responseHeadBuff = new byte[1024 * 50];

			responseHeadReadCount = HttpUtil.readHttpHead(responseHeadBuff,
					inFromPhp, key);
			String responseHead = ByteArrayUtil.toString(responseHeadBuff, 0,
					responseHeadReadCount);
			String[] responseHeaders = responseHead.split("\\r\\n");
			for (String responseHeader : responseHeaders) {
				if (responseHeader.startsWith("Connection")) {

				} else {
					outToBrowser.write(responseHeader.getBytes("US-ASCII"));
					outToBrowser.write("\r\n".getBytes("US-ASCII"));
				}
			}
			outToBrowser.write("Proxy-Connection: keep-alive"
					.getBytes("US-ASCII"));
			outToBrowser.write("\r\n".getBytes("US-ASCII"));

			outToBrowser.write(("Proxy-Server: " + Config.getIns()
					.getUserAgent()).getBytes("US-ASCII"));
			outToBrowser.write("\r\n".getBytes("US-ASCII"));

			outToBrowser.write(("X-PTP-Thread-Name: " + Thread.currentThread()
					.getName()).getBytes("US-ASCII"));
			outToBrowser.write("\r\n".getBytes("US-ASCII"));

			outToBrowser.write(("X-PTP-Remote-PHP: " + remotePhpUrl.toString())
					.getBytes("US-ASCII"));
			outToBrowser.write("\r\n".getBytes("US-ASCII"));

			outToBrowser.write("\r\n".getBytes("US-ASCII"));
			outToBrowser.flush();

			int readCount = -1;
			int phpByteSize = Integer.parseInt(Config.getIns().getValue(
					"ptp.buff.size", "1024"));
			byte[] phpByte = new byte[phpByteSize];

			while ((readCount = inFromPhp.read(phpByte, 0, phpByteSize)) != -1) {
				ByteArrayUtil.decrypt(phpByte, 0, readCount, key);
				outToBrowser.write(phpByte, 0, readCount);
				outToBrowser.flush();
				log.debug(ByteArrayUtil.toString(phpByte, 0, readCount));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			inFromPhp.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public abstract void process();

}