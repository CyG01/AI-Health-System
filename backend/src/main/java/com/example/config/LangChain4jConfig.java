package com.example.config;

import com.example.agent.HealthCoachAgent;
import com.example.agent.NutritionAgent;
import com.example.agent.PsychologyAgent;
import com.example.agent.SafetyReviewAgent;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;

/**
 * LangChain4j 配置 — Multi-Agent 架构。
 * 注册所有专家 Agent：健康教练 / 营养师 / 心理辅导 / 安全审查。
 */
@Configuration
public class LangChain4jConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String modelName;

    @Value("${deepseek.timeout:60000}")
    private int timeout;

    private final Environment environment;

    public LangChain4jConfig(Environment environment) {
        this.environment = environment;
    }

    private boolean isDevProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(profile) || "development".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ChatLanguageModel Bean — 指向 DeepSeek API（OpenAI 兼容）。
     */
    @Bean
    public ChatLanguageModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofMillis(timeout))
                .logRequests(isDevProfile())
                .logResponses(isDevProfile())
                .build();
    }

    /**
     * HealthCoachAgent — AI 健康教练 Agent。
     * 注册 PlanTools + ExerciseTools 为可调用工具，支持 Function Calling。
     */
    @Bean
    public HealthCoachAgent healthCoachAgent(
            ChatLanguageModel chatModel,
            com.example.agent.tool.PlanTools planTools,
            com.example.agent.tool.ExerciseTools exerciseTools) {
        return AiServices.builder(HealthCoachAgent.class)
                .chatLanguageModel(chatModel)
                .tools(planTools, exerciseTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    /**
     * NutritionAgent — 营养师专家 Agent。
     * 注册 NutritionTools 为可调用工具。
     */
    @Bean
    public NutritionAgent nutritionAgent(
            ChatLanguageModel chatModel,
            com.example.agent.tool.NutritionTools nutritionTools) {
        return AiServices.builder(NutritionAgent.class)
                .chatLanguageModel(chatModel)
                .tools(nutritionTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * PsychologyAgent — 心理健康 Agent。
     * 注册 PsychologyTools 为可调用工具。
     */
    @Bean
    public PsychologyAgent psychologyAgent(
            ChatLanguageModel chatModel,
            com.example.agent.tool.PsychologyTools psychologyTools) {
        return AiServices.builder(PsychologyAgent.class)
                .chatLanguageModel(chatModel)
                .tools(psychologyTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * SafetyReviewAgent — 安全审查 Agent。
     * 不注册 Tool（审查是单向判别），降低 temperature 以提升判定一致性。
     */
    @Bean
    public SafetyReviewAgent safetyReviewAgent(ChatLanguageModel chatModel) {
        return AiServices.builder(SafetyReviewAgent.class)
                .chatLanguageModel(OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .temperature(0.0)  // 审查需要确定性
                        .timeout(Duration.ofMillis(timeout))
                        .logRequests(false)
                        .logResponses(false)
                        .build())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(2))
                .build();
    }
}