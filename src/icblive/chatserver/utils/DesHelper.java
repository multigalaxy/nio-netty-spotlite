package icblive.chatserver.utils;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import com.alibaba.fastjson.util.Base64;


/**
 * @author sandcu
 * 
 */
public class DesHelper {

	public static final String ALGORITHM_DES = "DES/CBC/PKCS5Padding";

	/**
	 * DES算法，加密
	 * 
	 * @param data
	 *            待加密字符串
	 * @param key
	 *            加密私钥，长度不能够小于8位
	 * @return 加密后的Base64编码 异常
	 */
	public static String encode(String key, String data) throws Exception {
		return encode(key, data.getBytes());
	}

	private static String encode(String key, byte[] data) throws Exception {

		DESKeySpec dks = new DESKeySpec(key.getBytes());

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		// key的长度不能够小于8位字节
		Key secretKey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(
				"12345678".getBytes()));

		return "";
	}

	/**
	 * 获取编码后的值
	 * 
	 * @param key
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static String decode(String key, String data) throws Exception {
		byte[] datas;
		data = data.replaceAll("-", "+").replaceAll("_", "/");
		int count=data.length()%4;
		if(count!=0)
		{
			data+="====".substring(0,4-count);
		}
		datas = Base64.decodeFast(data);
		datas = decode(key, datas);
		return new String(datas);
	}

	public static byte[] decode(String key, byte[] data) throws Exception {

		DESKeySpec dks = new DESKeySpec(key.getBytes());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		// key的长度不能够小于8位字节
		Key secretKey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(
				"12345678".getBytes()));
		return cipher.doFinal(data);
	}
}
