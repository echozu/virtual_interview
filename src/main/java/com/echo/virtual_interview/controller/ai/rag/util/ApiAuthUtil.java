package com.echo.virtual_interview.controller.ai.rag.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

/**
 * 讯飞 API 鉴权签名生成工具类
 * 算法源自官方文档
 */
public class ApiAuthUtil {

    private static final char[] MD5_TABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 获取签名
     * @param appId    应用的AppId
     * @param secret   应用的Secret
     * @param ts       当前时间戳 (秒)
     * @return Base64编码后的签名
     */
    public static String getSignature(String appId, String secret, long ts) throws SignatureException {
        try {
            String auth = md5(appId + ts);
            return hmacSHA1Encrypt(auth, secret);
        } catch (Exception e) {
            throw new SignatureException("生成签名失败", e);
        }
    }

    private static String hmacSHA1Encrypt(String encryptText, String encryptKey) throws SignatureException {
        try {
            byte[] data = encryptKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(data, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKey);
            byte[] text = encryptText.getBytes(StandardCharsets.UTF_8);
            byte[] rawHmac = mac.doFinal(text);
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new SignatureException("HmacSHA1加密失败: " + e.getMessage(), e);
        }
    }

    private static String md5(String cipherText) throws NoSuchAlgorithmException {
        byte[] data = cipherText.getBytes();
        MessageDigest mdInst = MessageDigest.getInstance("MD5");
        mdInst.update(data);
        byte[] md = mdInst.digest();
        int j = md.length;
        char[] str = new char[j * 2];
        int k = 0;
        for (byte byte0 : md) {
            str[k++] = MD5_TABLE[byte0 >>> 4 & 0xf];
            str[k++] = MD5_TABLE[byte0 & 0xf];
        }
        return new String(str);
    }
}