package com.example.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 字段级加解密工具。
 *
 * 设计要点：
 * 1. AES-256-GCM 带认证标签（12 字节 IV + 128 位 Tag），防篡改 + 防泄漏
 * 2. 双密钥兼容解密：新密钥解密失败时自动回退到旧密钥，支持密钥轮换过渡期
 * 3. 密文格式：Base64(IV[12 bytes] + ciphertext + tag[16 bytes])
 * 4. 密钥从环境变量 AES_ENCRYPTION_KEY / AES_ENCRYPTION_LEGACY_KEY 加载
 * 5. 加密/解密失败时不阻塞业务：加密失败返回明文，解密失败返回密文原文
 */
@Slf4j
public final class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits, NIST 推荐
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int AES_KEY_LENGTH = 32; // 256 bits

    private static volatile SecretKey currentKey;
    private static volatile SecretKey legacyKey;
    private static volatile boolean initialized = false;

    /** 告警回调（由 EncryptionConfig 注入 WebhookNotifier） */
    private static volatile AlertCallback alertCallback;

    // ===== Phase 3 可观测性：操作计数器 =====
    private static final java.util.concurrent.atomic.AtomicLong encryptTotal = new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicLong encryptFailureCount = new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicLong decryptTotal = new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicLong decryptFailureCount = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 加密/解密失败时的告警回调接口。
     */
    @FunctionalInterface
    public interface AlertCallback {
        void alert(String severity, String ruleName, String message);
    }

    private AesEncryptor() {
    }

    // ---- 初始化 ----

    /**
     * 从环境变量加载密钥。在应用启动时由 {@link com.example.config.EncryptionConfig} 调用。
     */
    public static synchronized void init(String currentKeyBase64, String legacyKeyBase64) {
        if (currentKeyBase64 == null || currentKeyBase64.isBlank()) {
            throw new IllegalStateException("AES_ENCRYPTION_KEY 未设置！请在环境变量或 .env 文件中设置（Base64 编码的 32 字节密钥）");
        }
        byte[] keyBytes = Base64.getDecoder().decode(currentKeyBase64);
        if (keyBytes.length != AES_KEY_LENGTH) {
            throw new IllegalStateException(
                    "AES_ENCRYPTION_KEY 长度不正确，需要 " + AES_KEY_LENGTH + " 字节，当前 " + keyBytes.length + " 字节");
        }
        currentKey = new SecretKeySpec(keyBytes, "AES");

        if (legacyKeyBase64 != null && !legacyKeyBase64.isBlank()) {
            byte[] legacyBytes = Base64.getDecoder().decode(legacyKeyBase64);
            if (legacyBytes.length == AES_KEY_LENGTH) {
                legacyKey = new SecretKeySpec(legacyBytes, "AES");
                log.info("AesEncryptor 初始化完成，旧密钥已加载（兼容模式）");
            } else {
                log.warn("AES_ENCRYPTION_LEGACY_KEY 长度不正确，忽略旧密钥");
            }
        } else {
            log.info("AesEncryptor 初始化完成，单密钥模式");
        }
        initialized = true;
    }

    /**
     * 设置告警回调（由 EncryptionConfig 在启动时注入）。
     */
    public static void setAlertCallback(AlertCallback callback) {
        alertCallback = callback;
    }

    private static void fireAlert(String message) {
        log.error("[加密失败告警] {}", message);
        if (alertCallback != null) {
            try {
                alertCallback.alert("CRITICAL", "encryption_failure", message);
            } catch (Exception e) {
                log.warn("加密告警通知发送失败", e);
            }
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("AesEncryptor 未初始化，请检查 EncryptionConfig 是否已加载");
        }
    }

    // ---- 加解密入口 ----

    /**
     * 加密明文，返回 Base64 编码的密文。
     * 加密失败时返回原始明文（不阻塞业务），同时触发告警。
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        ensureInitialized();
        encryptTotal.incrementAndGet();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, currentKey, spec);
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext (已含 tag)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            encryptFailureCount.incrementAndGet();
            String msg = String.format("AES加密失败，降级返回明文（数据长度=%d）: %s",
                    plain.length(), e.getMessage());
            fireAlert(msg);
            // 返回明文，不阻塞业务
            return plain;
        }
    }

    /**
     * 解密密文，支持新旧双密钥兼容。
     * 解密失败时返回密文原文（不阻塞业务），同时触发告警。
     */
    public static String decrypt(String cipher) {
        if (cipher == null || cipher.isEmpty()) {
            return cipher;
        }
        ensureInitialized();
        decryptTotal.incrementAndGet();
        try {
            return decryptWithKey(cipher, currentKey);
        } catch (Exception e) {
            if (legacyKey != null) {
                log.debug("新密钥解密失败，尝试旧密钥: {}", e.getMessage());
                try {
                    return decryptWithKey(cipher, legacyKey);
                } catch (Exception ex) {
                    decryptFailureCount.incrementAndGet();
                    String msg = String.format("新旧密钥均解密失败，降级返回密文原文: %s", ex.getMessage());
                    fireAlert(msg);
                    return cipher;
                }
            }
            decryptFailureCount.incrementAndGet();
            String msg = String.format("解密失败，降级返回密文原文: %s", e.getMessage());
            fireAlert(msg);
            return cipher;
        }
    }

    /**
     * 用指定密钥解密。
     */
    private static String decryptWithKey(String cipherBase64, SecretKey key) throws Exception {
        byte[] data = Base64.getDecoder().decode(cipherBase64);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plain = cipher.doFinal(ciphertext);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /**
     * 生成一个新的 AES-256 密钥（Base64 编码），供运维人员手动使用。
     */
    public static String generateKeyBase64() {
        byte[] keyBytes = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    // ===== Phase 3 可观测性：操作计数器查询 =====

    /** 加密操作总数 */
    public static long getEncryptTotal() { return encryptTotal.get(); }

    /** 加密失败次数 */
    public static long getEncryptFailureCount() { return encryptFailureCount.get(); }

    /** 解密操作总数 */
    public static long getDecryptTotal() { return decryptTotal.get(); }

    /** 解密失败次数 */
    public static long getDecryptFailureCount() { return decryptFailureCount.get(); }
}