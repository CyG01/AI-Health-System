package com.example.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson 全局 XSS 过滤配置
 * 替换 String 反序列化器，在 JSON 反序列化阶段统一过滤 XSS 注入
 */
@Configuration
public class JacksonXssConfig {

    /**
     * XSS 专用 String 反序列化器
     */
    static class XssStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = StringDeserializer.instance.deserialize(p, ctxt);
            return cleanXss(value);
        }

        private String cleanXss(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            return value
                    .replaceAll("(?i)<script.*?>.*?</script>", "")
                    .replaceAll("(?i)<script.*?>", "")
                    .replaceAll("(?i)sr[\\s]*c[\\s]*=[\\s]*['\"].*?['\"]", "")
                    .replaceAll("(?i)eval\\(.*?\\)", "")
                    .replaceAll("(?i)expression\\(.*?\\)", "")
                    .replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)vbscript:", "")
                    .replaceAll("(?i)onload(.*?)=", "")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xssJacksonCustomizer() {
        return builder -> {
            SimpleModule xssModule = new SimpleModule("XssStringModule");
            xssModule.addDeserializer(String.class, new XssStringDeserializer());
            builder.modulesToInstall(xssModule);
        };
    }
}
