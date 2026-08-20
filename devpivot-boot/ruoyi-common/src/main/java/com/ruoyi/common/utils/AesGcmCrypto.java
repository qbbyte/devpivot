package com.ruoyi.common.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * API 密钥 AES/GCM 加密工具（单例 Bean）。
 *
 * 密钥来源：环境变量 / 配置项 devpivot.apikey.aes-secret，要求为 32 字节(256bit) 的 BASE64 字符串。
 * 生产环境必须配置；未配置时回退到内置开发密钥并打出显眼告警（仅限本地，密钥可被反编译获取）。
 *
 * 存储格式：ENC:<base64(iv(12) + ciphertext+tag)>。解密时对无 ENC: 前缀的值按明文兼容返回，
 * 以便存量明文数据在首次更新后被自然加密，无需一次性迁移脚本。
 *
 * @author devpivot
 * @date 2026-08-19
 */
@Component
public class AesGcmCrypto
{
    private static final Logger log = LoggerFactory.getLogger(AesGcmCrypto.class);

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_LEN = 128;
    private static final String PREFIX = "ENC:";
    /** 仅本地开发回退密钥（32 字节 BASE64），切勿用于生产 */
    private static final String DEV_FALLBACK = "ervn5eIc8abY3wbuhke9kgB76lSHgeAHgVZ0fQ5DruY=";

    private final SecretKey key;
    private final boolean usingFallback;

    public AesGcmCrypto(@Value("${devpivot.apikey.aes-secret:}") String secret)
    {
        byte[] keyBytes;
        if (StringUtils.isNotBlank(secret))
        {
            keyBytes = Base64.getDecoder().decode(secret.trim());
            this.usingFallback = false;
        }
        else
        {
            keyBytes = Base64.getDecoder().decode(DEV_FALLBACK);
            this.usingFallback = true;
            log.warn("========================================================================");
            log.warn(" devpivot.apikey.aes-secret 未配置，已使用内置『开发』密钥加密 API Key！");
            log.warn(" 该密钥随代码分发、可被反编译获取，仅可用于本地开发。");
            log.warn(" 生产环境务必通过环境变量设置 32 字节(256bit) BASE64 密钥：");
            log.warn("   export DEVPIVOT_APIKEY_AES_SECRET=$(openssl rand -base64 32)");
            log.warn("========================================================================");
        }
        if (keyBytes.length != 32)
        {
            throw new IllegalStateException(
                    "devpivot.apikey.aes-secret 必须为 32 字节(256bit) 的 BASE64 编码密钥，当前解码长度=" + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** 加密明文；空值原样返回；已是密文(ENC: 前缀)幂等返回 */
    public String encrypt(String plain)
    {
        if (plain == null)
        {
            return null;
        }
        if (plain.startsWith(PREFIX))
        {
            return plain;
        }
        try
        {
            byte[] iv = new byte[IV_LEN];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(iv.length + ct.length);
            bb.put(iv).put(ct);
            return PREFIX + Base64.getEncoder().encodeToString(bb.array());
        }
        catch (Exception e)
        {
            log.error("API Key 加密失败", e);
            throw new ServiceException("密钥加密失败");
        }
    }

    /** 解密；空值原样返回；无 ENC: 前缀的存量明文兼容返回 */
    public String decrypt(String stored)
    {
        if (stored == null)
        {
            return null;
        }
        if (!stored.startsWith(PREFIX))
        {
            return stored;
        }
        try
        {
            byte[] raw = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            ByteBuffer bb = ByteBuffer.wrap(raw);
            byte[] iv = new byte[IV_LEN];
            bb.get(iv);
            byte[] ct = new byte[bb.remaining()];
            bb.get(ct);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.error("API Key 解密失败", e);
            throw new ServiceException("密钥解密失败");
        }
    }

    /** 脱敏：仅保留后 4 位，其余以 * 替代（入参应为已解密的明文） */
    public static String maskKey(String plain)
    {
        if (StringUtils.isBlank(plain))
        {
            return "";
        }
        if (plain.startsWith(PREFIX))
        {
            return "********";
        }
        if (plain.length() <= 4)
        {
            return plain;
        }
        return "****" + plain.substring(plain.length() - 4);
    }

    public boolean isUsingFallback()
    {
        return usingFallback;
    }
}
