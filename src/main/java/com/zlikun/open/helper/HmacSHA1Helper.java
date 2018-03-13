package com.zlikun.open.helper;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 11:10
 */
public class HmacSHA1Helper {

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptText 被签名的字符串
     * @param encryptKey  密钥
     * @return 返回被加密后的字符串
     * @throws Exception
     */
    public static byte[] encrypt(String encryptText, String encryptKey) throws Exception {
        byte[] data = encryptKey.getBytes(ENCODING);
        return encrypt(data, encryptKey);
    }

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptData 被签名的字符串
     * @param encryptKey  密钥
     * @return 返回被加密后的字符串
     * @throws Exception
     */
    public static byte[] encrypt(byte[] encryptData, String encryptKey) throws Exception {
        byte[] data = encryptKey.getBytes(ENCODING);
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, MAC_NAME);
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(MAC_NAME);
        // 用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        // 完成 Mac 操作
        return mac.doFinal(encryptData);
    }

}
