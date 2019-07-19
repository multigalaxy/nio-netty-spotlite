package icblive.chatserver.utils;
/**
 * 
 */
//package com.changba.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xiaol
 *
 */
public class GzipUtils {
	static Logger logger = LogManager.getLogger("default");
	
	public static byte[] encode(String data){
		   ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		   
	        // 压缩  
	        GZIPOutputStream gos = null;
	        byte[] output = null;
			try {
				gos = new GZIPOutputStream(baos);
		        byte[] in = data.getBytes("UTF8");
		        gos.write(in, 0, in.length);  
		        gos.finish();  
		        output = baos.toByteArray();  
 
			} catch (IOException e) {
				logger.error("GZIP error: " + data, e);
			}finally{
				if (baos!=null){
			        try {
						baos.flush();
						baos.close(); 
					} catch (IOException e) {}  
			        
				}
				if (gos!=null){
					try {
						gos.close();
					} catch (IOException e) {}
				}
			}

	        return output; 
	}
	public static String decode (byte[] in){
		ByteArrayInputStream bais = new ByteArrayInputStream(in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		GZIPInputStream gis = null;
		 byte[] output = null;
		try {
			gis = new GZIPInputStream(bais);
			 int count;  
		        byte buff[] = new byte[1024];  
		        while ((count = gis.read(buff, 0, 1024)) != -1) {  
		        	baos.write(buff, 0, count);  
		        }  
		        output = baos.toByteArray();  
		} catch (IOException e) {
			return "";
		}finally{
			if (gis!=null){
				try {
					gis.close();
				} catch (IOException e) {}
			}
			if (bais!=null){
				try {
					bais.close();
				} catch (IOException e) {}
			}
			if (baos!=null){
				try {
					baos.close();
				} catch (IOException e) {}
			}
		}
		
		String res = "";
		try {
			res = new String(output, "UTF8");
		} catch (UnsupportedEncodingException e) {
		}
		return res;
	}
	public static boolean isGziped (byte[] data){

		if(data!=null && data.length>2 && data[0] ==31 && data[1] ==-117){
			return true;
		}
		return false;
	}
	
	public static void main(String s[]){
		byte[] in = encode("高洁xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		System.out.println(in);
		System.out.println(isGziped(in));
		String out = decode(in);
		System.out.println(out);
	}
}
