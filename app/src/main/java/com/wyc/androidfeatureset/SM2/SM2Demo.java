package com.wyc.androidfeatureset.SM2;

import org.bouncycastle.util.Arrays;

public class SM2Demo {

	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}

		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}


	public static void main(String[] args) throws Exception {
		SM2 clz = SM2.getInstance();
		byte [] sourceData = "userData".getBytes();

		/* 密管密钥测试 */
		String x = "E0E2E37F11901483CDFBC47F489D87D5D78C55DD7F919B73DEA83007748668B7";
		String y = "871A1BA9608F156E25B7D64C7821379BAC1E2C591D5A50FF311D1AAE026C1DAE";
		String d = "D25595C27BC0E4C678533F06D9D7BA66EECDBCED47268112B48E5CEA4563EC00";

		{   // 密管密钥自签自验
			byte[] sign = clz.SM2Sign(hexStringToBytes(d), sourceData);
			System.out.println("SignData：" + new String(sign));
			boolean verify = clz.SM2Verify(hexStringToBytes(x), hexStringToBytes(y), sourceData, sign);
			System.out.println("VerifyResult：" + verify);
		}

		{   // 验证密管签名数据
			byte[] sign = "MEYCIQDQkU1OuQMEXTiu/mRz7Lzq8/vdVYB+zI4ymBsl5KzW7AIhAKpn19GiOgZpLTCH4IvonROL5Yj3YtUjhIDXYzQGjRpW".getBytes();
			boolean verify = clz.SM2Verify(hexStringToBytes(x), hexStringToBytes(y), sourceData, sign);
			System.out.println("VerifyResult：" + verify);
		}

		System.out.println("===========================================================");
		
		byte [] bigData = new byte[1024*10];
		Arrays.fill(bigData, (byte)0x49);
		{   // 第一组随机密钥自签自验
			String xRandom = "BB34D657EE7E8490E66EF577E6B3CEA28B739511E787FB4F71B7F38F241D87F1";
			String yRandom = "8A5A93DF74E90FF94F4EB907F271A36B295B851F971DA5418F4915E2C1A23D6E";
			String dRandom = "0B1CE43098BC21B8E82B5C065EDB534CB86532B1900A49D49F3C53762D2997FA";
			
			byte[] sign = clz.SM2Sign(hexStringToBytes(dRandom), bigData);
			System.out.println("SignData：" + new String(sign));
			boolean verify = clz.SM2Verify(hexStringToBytes(xRandom), hexStringToBytes(yRandom), bigData, sign);
			System.out.println("VerifyResult：" + verify);
		}
		
		{   // 第二组随机密钥自签自验
			String xRandom = "021091496615CF1C69B631D393C68BECCAFCCEAC5527667E95328F8ABF5CF5A4";
			String yRandom = "03A2A7B640E67E861B336FC7589486257A7D841159D11696C3F4296E0F21A0D5";
			String dRandom = "7CD798AF4F6643E844591902569A4E35514A21E9866D537892115AC21494C550";
			
			byte[] sign = clz.SM2Sign(hexStringToBytes(dRandom), bigData);
			System.out.println("SignData：" + new String(sign));
			boolean verify = clz.SM2Verify(hexStringToBytes(xRandom), hexStringToBytes(yRandom), bigData, sign);
			System.out.println("VerifyResult：" + verify);
		}
		
		{   // 第三组随机密钥自签自验
			String xRandom = "86AB9805266A1C88F4DF54BCAF1E51A91BE41E13CCD252F898BDBBEE958A2FB7";
			String yRandom = "4D164CD51CCEAA2D1D399B8629A79EBA60328E702BF37982FEFB859ED9F80F7B";
			String dRandom = "0407CFBCFF7AD740DD7B11A199567018C5F7B8E474F1AAECC0C0EE01241FE410";
			
			byte[] sign = clz.SM2Sign(hexStringToBytes(dRandom), bigData);
			System.out.println("SignData：" + new String(sign));
			boolean verify = clz.SM2Verify(hexStringToBytes(xRandom), hexStringToBytes(yRandom), bigData, sign);
			System.out.println("VerifyResult：" + verify);
		}
	}
}
