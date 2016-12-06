package com.infosys.submission.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public class AES {

	private static String encoding = "UTF-8";
	
	public static String encryptAES(String valueToEncrypt, String password) throws Exception {
		byte[] preSharedKey = password.getBytes();
		byte[] iv = password.getBytes();
		byte[] data = valueToEncrypt.getBytes(AES.encoding);
		SecretKey aesKey = new SecretKeySpec(preSharedKey, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
		byte[] output = cipher.doFinal(data);
		String mmm = new String(Hex.encodeHex(output));
		return (mmm);
	}

	public static String decryptAES(String valueToDecrypt, String password) throws Exception {

		int len = valueToDecrypt.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(valueToDecrypt.charAt(i), 16) << 4)
					+ Character.digit(valueToDecrypt.charAt(i + 1), 16));
		}

		byte[] keyBytes = password.getBytes(AES.encoding);
		byte[] ivBytes = keyBytes;
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		SecretKeySpec spec = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, spec, ivSpec);
		data = cipher.doFinal(data);
		return (new String(data));

	}

	public static void main(String[] args) {
		try {
		String strEncryptionPassword="mcAX65PTadrrsKQ3";
		String enc = AES.encryptAES("ShaliniReddy_B",strEncryptionPassword);
//		String enc = "BFFAFBB11A2DFCA7BB2BDB7E552D766FCE3E507C7BFB369A8C6D05C0FF2C6977";
	
			System.out.println(enc);
			System.out.println(AES.decryptAES(enc,strEncryptionPassword));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
