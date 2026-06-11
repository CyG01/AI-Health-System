package com.example.util;

import com.example.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 响应 JSON 解析器 —— 防御性加固
 *
 * 支持以下格式的自动提取：
 * 1. 纯 JSON 字符串
 * 2. Markdown ```json ... ``` 代码块包裹
 * 3. JSON 前后有额外文本（正则提取最外层 JSON Object）
 */
public class AiResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 匹配最外层 JSON Object —— 从 { 开始匹配到对应的 }
     */
    private static final Pattern JSON_BLOCK = Pattern.compile(
            "\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*}", Pattern.DOTALL
    );

    /**
     * 匹配最外层 JSON Array
     */
    private static final Pattern JSON_ARRAY = Pattern.compile(
            "\\[[^\\[\\]]*(?:\\[[^\\[\\]]*][^\\[\\]]*)*]", Pattern.DOTALL
    );

    /**
     * 从 AI 返回值中安全提取 JSON
     *
     * @param aiResponse AI 返回的原始文本
     * @return 解析后的 JsonNode
     * @throws BusinessException 如果无法解析
     */
    public static JsonNode extractJson(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            throw new BusinessException("AI返回内容为空，无法解析");
        }

        // 1. 先尝试直接解析
        try {
            return mapper.readTree(aiResponse.trim());
        } catch (Exception ignored) {
            // 继续尝试其他方式
        }

        // 2. 剥离 markdown 代码块标记
        String cleaned = aiResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "");

        // 3. 尝试直接解析清理后的内容
        try {
            return mapper.readTree(cleaned.trim());
        } catch (Exception ignored) {
            // 继续尝试
        }

        // 4. 正则提取最外层 JSON Object
        Matcher objectMatcher = JSON_BLOCK.matcher(cleaned);
        if (objectMatcher.find()) {
            try {
                String jsonCandidate = objectMatcher.group();
                return mapper.readTree(jsonCandidate);
            } catch (Exception ignored) {
                // 继续尝试
            }
        }

        // 5. 尝试提取 JSON Array
        Matcher arrayMatcher = JSON_ARRAY.matcher(cleaned);
        if (arrayMatcher.find()) {
            try {
                return mapper.readTree(arrayMatcher.group());
            } catch (Exception ignored) {
                // 继续尝试
            }
        }

        // 6. 尝试找到第一个 { 到最后一个 } 的内容
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            try {
                String jsonCandidate = cleaned.substring(firstBrace, lastBrace + 1);
                return mapper.readTree(jsonCandidate);
            } catch (Exception ignored) {
                // 彻底失败
            }
        }

        throw new BusinessException("AI返回内容格式异常，无法解析为JSON");
    }

    /**
     * 从 AI 返回值中提取纯文本（用于聊天场景）
     * 如果内容被 JSON 包裹，自动提取文本部分
     */
    public static String extractText(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return "";
        }
        // 尝试去除可能的代码块标记
        return aiResponse
                .replaceAll("```[a-zA-Z]*\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }
}