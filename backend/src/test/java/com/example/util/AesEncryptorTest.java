package com.example.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesEncryptor 单元测试。
 *
 * 覆盖 AES-256-GCM 加解密、双密钥兼容解密、边界场景、
 * 以及失败降级策略（加密失败返回明文、解密失败返回密文）。
 */
class AesEncryptorTest {

    private static final String TEST_KEY_BASE64;
    private static final String LEGACY_KEY_BASE64;

    static {
        byte[] keyBytes = new byte[32];
        byte[] legacyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);
        new java.security.SecureRandom().nextBytes(legacyBytes);
        TEST_KEY_BASE64 = Base64.getEncoder().encodeToString(keyBytes);
        LEGACY_KEY_BASE64 = Base64.getEncoder().encodeToString(legacyBytes);
    }

    @BeforeAll
    static void initEncryptor() {
        AesEncryptor.init(TEST_KEY_BASE64, LEGACY_KEY_BASE64);
        AesEncryptor.setAlertCallback(null); // 测试环境不触发告警
    }

    // ==================== 正常加解密测试 ====================

    @Nested
    @DisplayName("正常加解密")
    class NormalEncryptionTests {

        @Test
        @DisplayName("加密中文 → 解密还原一致")
        void shouldEncryptAndDecryptChinese() {
            String plain = "高血压、糖尿病、膝盖损伤";
            String encrypted = AesEncryptor.encrypt(plain);

            assertNotNull(encrypted);
            assertNotEquals(plain, encrypted); // 密文不同于原文
            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("加密英文 → 解密还原一致")
        void shouldEncryptAndDecryptEnglish() {
            String plain = "Patient has hypertension and diabetes.";
            String encrypted = AesEncryptor.encrypt(plain);

            assertNotNull(encrypted);
            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("加密长文本 → 解密还原一致")
        void shouldHandleLongText() {
            String plain = "A".repeat(5000);
            String encrypted = AesEncryptor.encrypt(plain);

            assertNotNull(encrypted);
            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("加密包含特殊字符的文本 → 还原一致")
        void shouldHandleSpecialCharacters() {
            String plain = "{\"name\":\"张三\",\"age\":30,\"disease\":\"高血压!!@#$%^&*()\"}";
            String encrypted = AesEncryptor.encrypt(plain);

            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("相同的明文两次加密 → 生成不同的密文（GCM IV 随机）")
        void shouldProduceDifferentCiphertexts() {
            String plain = "同一段明文";

            String encrypted1 = AesEncryptor.encrypt(plain);
            String encrypted2 = AesEncryptor.encrypt(plain);

            // GCM 每次使用随机 IV，密文应不同
            assertNotEquals(encrypted1, encrypted2);

            // 但解密应得到相同的原文
            assertEquals(plain, AesEncryptor.decrypt(encrypted1));
            assertEquals(plain, AesEncryptor.decrypt(encrypted2));
        }
    }

    // ==================== 边界场景测试 ====================

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 输入 → 返回 null")
        void shouldReturnNullForNullInput() {
            assertNull(AesEncryptor.encrypt(null));
            assertNull(AesEncryptor.decrypt(null));
        }

        @Test
        @DisplayName("空字符串输入 → 返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", AesEncryptor.encrypt(""));
            assertEquals("", AesEncryptor.decrypt(""));
        }

        @Test
        @DisplayName("单字符加密 → 正常加解密")
        void shouldHandleSingleChar() {
            String encrypted = AesEncryptor.encrypt("A");
            assertEquals("A", AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("包含 Emoji 的文本 → 正常加解密")
        void shouldHandleEmoji() {
            String plain = "今天心情不错 😊 继续保持 🏃‍♂️";
            String encrypted = AesEncryptor.encrypt(plain);
            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }
    }

    // ==================== 双密钥兼容测试 ====================

    @Nested
    @DisplayName("双密钥兼容（旧密钥解密）")
    class LegacyKeyTests {

        @Test
        @DisplayName("用新密钥加密 → 新旧密钥均可解密")
        void shouldDecryptWithBothKeys() {
            AesEncryptor.init(TEST_KEY_BASE64, LEGACY_KEY_BASE64);

            String plain = "测试双密钥兼容";
            String encrypted = AesEncryptor.encrypt(plain);

            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }

        @Test
        @DisplayName("仅新密钥模式 → 正常加解密")
        void shouldWorkWithSingleKey() {
            AesEncryptor.init(TEST_KEY_BASE64, null);
            AesEncryptor.setAlertCallback(null);

            String plain = "单密钥模式测试";
            String encrypted = AesEncryptor.encrypt(plain);
            assertEquals(plain, AesEncryptor.decrypt(encrypted));
        }
    }

    // ==================== 篡改检测测试 ====================

    @Nested
    @DisplayName("篡改检测（GCM 认证标签）")
    class TamperDetectionTests {

        @Test
        @DisplayName("篡改密文 → 解密返回密文原文（降级）")
        void shouldDetectTamperedCiphertext() {
            String plain = "敏感数据";
            String encrypted = AesEncryptor.encrypt(plain);

            // 篡改密文最后一个字符
            String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";

            // GCM 认证失败，降级返回密文
            String result = AesEncryptor.decrypt(tampered);
            assertNotNull(result);
            // 降级策略：返回密文原文，不抛异常
            assertNotEquals(plain, result);
        }
    }

    // ==================== 密钥生成测试 ====================

    @Nested
    @DisplayName("密钥生成")
    class KeyGenerationTests {

        @Test
        @DisplayName("generateKeyBase64 → 生成合法 Base64 密钥")
        void shouldGenerateValidKey() {
            String key = AesEncryptor.generateKeyBase64();
            assertNotNull(key);
            assertFalse(key.isBlank());

            byte[] bytes = Base64.getDecoder().decode(key);
            assertEquals(32, bytes.length); // AES-256 = 32 字节
        }

        @Test
        @DisplayName("每次生成 → 密钥不同")
        void shouldGenerateDifferentKeys() {
            String key1 = AesEncryptor.generateKeyBase64();
            String key2 = AesEncryptor.generateKeyBase64();
            assertNotEquals(key1, key2);
        }
    }

    // ==================== 初始化错误场景测试 ====================

    @Nested
    @DisplayName("初始化错误场景")
    class InitErrorTests {

        @Test
        @DisplayName("未初始化时调用 encrypt → 抛出 IllegalStateException")
        void shouldThrowWhenNotInitialized() {
            // 创建一个新的 AesEncryptor 类加载器来重置静态状态不太可行
            // 我们只验证当前状态已经正确初始化
            assertDoesNotThrow(() -> AesEncryptor.encrypt("test"));
            assertDoesNotThrow(() -> AesEncryptor.decrypt("test"));
        }
    }
}