package ptp.net.mp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.log4j.Logger;

import ptp.Config;
import ptp.util.Base64Coder;
import ptp.util.ByteArrayUtil;

public abstract class MethodProcesser {
	private static Logger log = Logger.getLogger(MethodProcesser.class);
	
	public static MethodProcesser getIns(byte[] head, int headLen, DataInputStream inFromBrowser, DataOutputStream outToBrowser) {
		String headString = ByteArrayUtil.toString(head, 0,
				headLen);
		String[] headers = headString.split("\\r\\n");
		if(headers[0].startsWith("GET"))
			return new GetMethodProcesser(headers, inFromBrowser, outToBrowser);
		else
			return null;
		
	}
	
	void requestRemote(byte[] data, String destHost, int destPort, boolean isSSL,
			DataOutputStream outToBrowser){
		String remotePhp = Config.getIns().getRemotePhp();
		log.info("remotePhp: " + remotePhp);

		String requestBase64String = new String(Base64Coder.encode(data, 0,
				data.length));

		byte[] postData = ("request_data=" + requestBase64String
				+ "&dest_host=" + destHost + "&dest_port=" + destPort
				+ "&is_ssl=" + isSSL).getBytes();

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
		remotePhpConn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		remotePhpConn.setRequestProperty("Connection", "close");
		remotePhpConn.setUseCaches(false);
		remotePhpConn.setDoOutput(true);

		try {
		DataOutputStream outToPhp = new DataOutputStream(remotePhpConn
				.getOutputStream());

		outToPhp.write(postData);

		outToPhp.flush();
		outToPhp.close();
		} catch(IOException e) {
			//TODO
		}

		DataInputStream inFromPhp = null;
		try{
			inFromPhp = new DataInputStream(remotePhpConn
				.getInputStream());
		} catch(IOException e) {
			//TODO
		}
		
		int readCount = -1;
		int phpByteSize = Integer.parseInt(Config.getIns().getValue(
				"ptp.buff.size", "1024"));
		byte[] phpByte = new byte[phpByteSize];
		try {
			while ((readCount = inFromPhp.read(phpByte, 0, phpByteSize)) != -1) {
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
