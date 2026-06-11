package com.example.service;

import com.example.BaseTest;
import com.example.model.FunctionCallResult;
import com.example.model.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeepSeek Function Calling 集成测试。
 * 验证 DeepSeek API 协议兼容性：
 * 1. 工具定义格式（DeepSeek 兼容 OpenAI protocol）
 * 2. 多轮工具调用循环
 * 3. 工具参数解析
 * 4. 工具执行结果格式
 * 5. 错误处理
 * <p>
 * 使用 WireMock 模拟 DeepSeek API 响应。
 */
@DisplayName("DeepSeek Function Calling 集成测试")
class DeepSeekFunctionCallingIntegrationTest extends BaseTest {

    private static WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("deepseek.base-url", () ->
                "http://localhost:" + wireMockServer.port() + "/v1");
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetMocks() {
        WireMock.reset();
    }

    // ==================== 协议兼容性 ====================

    @Nested
    @DisplayName("协议兼容性")
    class ProtocolCompatibility {

        @Test
        @DisplayName("工具定义序列化：输出格式兼容 DeepSeek/OpenAI")
        void shouldBuildToolDefinitionInOpenAiFormat() {
            ToolDefinition.Parameters params = ToolDefinition.Parameters.builder()
                    .type("object")
                    .property("age", ToolDefinition.PropertyDef.builder()
                            .type("integer")
                            .description("用户年龄")
                            .build())
                    .property("gender", ToolDefinition.PropertyDef.builder()
                            .type("string")
                            .description("用户性别")
                            .build())
                    .required(List.of("age"))
                    .build();

            ToolDefinition tool = ToolDefinition.of("calculateBmr",
                    "根据身高体重年龄计算基础代谢率", params);

            List<Map<String, Object>> built = deepSeekService.buildToolDefinitions(List.of(tool));

            assertThat(built).hasSize(1);
            Map<String, Object> def = built.get(0);
            assertThat(def.get("type")).isEqualTo("function");

            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) def.get("function");
            assertThat(func.get("name")).isEqualTo("calculateBmr");
            assertThat(func.get("description")).toString().contains("基础代谢率");

            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) func.get("parameters");
            assertThat(parameters.get("type")).isEqualTo("object");
            assertThat(parameters.containsKey("properties")).isTrue();
            assertThat(parameters.containsKey("required")).isTrue();

            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) parameters.get("required");
            assertThat(required).contains("age");
        }

        @Test
        @DisplayName("strict mode 标记应正确序列化")
        void shouldSerializeStrictModeFlag() {
            ToolDefinition tool = ToolDefinition.builder()
                    .function(ToolDefinition.FunctionDef.builder()
                            .name("testStrict")
                            .description("测试strict")
                            .parameters(ToolDefinition.Parameters.builder().build())
                            .strict(true)
                            .build())
                    .build();

            List<Map<String, Object>> built = deepSeekService.buildToolDefinitions(List.of(tool));

            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) built.get(0).get("function");
            assertThat(func.get("strict")).isEqualTo(true);
        }

        @Test
        @DisplayName("tool_calls 提取：DeepSeek JSON 响应正确解析")
        void shouldExtractToolCallsFromDeepSeekResponse() throws Exception {
            String jsonResponse = """
                    {
                      "choices": [{
                        "finish_reason": "tool_calls",
                        "message": {
                          "role": "assistant",
                          "tool_calls": [{
                            "id": "call_abc123",
                            "type": "function",
                            "function": {
                              "name": "calculateBmr",
                              "arguments": "{\\"age\\": 30, \\"gender\\": \\"male\\"}"
                            }
                          }]
                        }
                      }],
                      "usage": {"prompt_tokens": 100, "completion_tokens": 50, "total_tokens": 150}
                    }
                    """;

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode tcNode = root.path("choices").get(0).path("message").path("tool_calls");

            List<FunctionCallResult.ToolCallRequest> extracted = deepSeekService.extractToolCalls(tcNode);

            assertThat(extracted).hasSize(1);
            FunctionCallResult.ToolCallRequest call = extracted.get(0);
            assertThat(call.getId()).isEqualTo("call_abc123");
            assertThat(call.getName()).isEqualTo("calculateBmr");
            assertThat(call.getArgumentsNode()).isNotNull();
            assertThat(call.getArgumentsNode().path("age").asInt()).isEqualTo(30);
            assertThat(call.getArgumentsNode().path("gender").asText()).isEqualTo("male");
        }

        @Test
        @DisplayName("多个并行工具调用应全部提取")
        void shouldExtractParallelToolCalls() throws Exception {
            String json = """
                    {
                      "tool_calls": [
                        {"id":"c1","type":"function","function":{"name":"t1","arguments":"{\\"a\\":1}"}},
                        {"id":"c2","type":"function","function":{"name":"t2","arguments":"{\\"b\\":2}"}}
                      ]
                    }
                    """;

            JsonNode root = objectMapper.readTree(json);
            List<FunctionCallResult.ToolCallRequest> extracted = deepSeekService.extractToolCalls(
                    root.path("tool_calls"));

            assertThat(extracted).hasSize(2);
            assertThat(extracted.get(0).getName()).isEqualTo("t1");
            assertThat(extracted.get(1).getName()).isEqualTo("t2");
        }

        @Test
        @DisplayName("非 function 类型 tool_call 应跳过")
        void shouldSkipNonFunctionToolTypes() throws Exception {
            String json = """
                    {
                      "tool_calls": [{
                        "id": "call_1",
                        "type": "code_interpreter",
                        "function": {"name": "test", "arguments": "{}"}
                      }]
                    }
                    """;

            JsonNode root = objectMapper.readTree(json);
            List<FunctionCallResult.ToolCallRequest> extracted = deepSeekService.extractToolCalls(
                    root.path("tool_calls"));

            assertThat(extracted).isEmpty();
        }
    }

    // ==================== 完整流程 ====================

    @Nested
    @DisplayName("完整 Function Calling 流程")
    class FullFlow {

        @Test
        @DisplayName("单工具调用一轮完成")
        void shouldCompleteSingleToolCallRound() {
            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .inScenario("single-tool")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant",
                                    "tool_calls":[{"id":"call_001","type":"function",
                                    "function":{"name":"calculateHR","arguments":"{\\"age\\":30,\\"restingHr\\":70}"}}]}}],
                                    "usage":{"prompt_tokens":80,"completion_tokens":40}}
                                    """)
                            .withStatus(200))
                    .willSetStateTo("tool-executed"));

            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .inScenario("single-tool")
                    .whenScenarioStateIs("tool-executed")
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"choices":[{"finish_reason":"stop","message":{"role":"assistant",
                                    "content":"你的最大心率预计是190bpm，燃脂区间115-132bpm。"}}],
                                    "usage":{"prompt_tokens":150,"completion_tokens":60}}
                                    """)
                            .withStatus(200)));

            ToolDefinition.Parameters params = ToolDefinition.Parameters.builder()
                    .property("age", ToolDefinition.PropertyDef.builder().type("integer")
                            .description("年龄").build())
                    .property("restingHr", ToolDefinition.PropertyDef.builder().type("integer")
                            .description("静息心率").build())
                    .required(List.of("age"))
                    .build();

            List<ToolDefinition> tools = List.of(ToolDefinition.of(
                    "calculateHR", "根据年龄计算靶心率区间", params));

            com.example.model.ToolExecutor executor = (callId, name, args) ->
                    "心率区间计算结果：热身区 130-142 bpm，燃脂区 142-154 bpm";

            FunctionCallResult result = deepSeekService.functionCall(
                    "你是健康教练", "我30岁，静息70，帮我算心率区间", tools, executor, 5);

            assertThat(result).isNotNull();
            assertThat(result.getToolCalls()).hasSize(1);
            assertThat(result.getToolCalls().get(0).getName()).isEqualTo("calculateHR");
            assertThat(result.getContent()).contains("190bpm");
            assertThat(result.isFinished()).isTrue();
        }

        @Test
        @DisplayName("无需工具时应直接返回文本")
        void shouldReturnDirectTextWhenNoToolNeeded() {
            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"choices":[{"finish_reason":"stop","message":{"role":"assistant",
                                    "content":"每天建议饮水1500-2000毫升。"}}],
                                    "usage":{"prompt_tokens":50,"completion_tokens":30}}
                                    """)
                            .withStatus(200)));

            List<ToolDefinition> tools = List.of(ToolDefinition.of("search",
                    "查询知识", ToolDefinition.Parameters.builder().build()));

            com.example.model.ToolExecutor executor = (id, name, args) -> "never";

            FunctionCallResult result = deepSeekService.functionCall(
                    "健康顾问", "每天喝多少水？", tools, executor);

            assertThat(result.getToolCalls()).isEmpty();
            assertThat(result.getContent()).contains("1500-2000");
        }

        @Test
        @DisplayName("工具执行异常应捕获并继续返回模型下一轮回复")
        void shouldHandleToolErrorAndContinue() {
            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .inScenario("error-tool")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant",
                                    "tool_calls":[{"id":"call_fail","type":"function",
                                    "function":{"name":"failingTool","arguments":"{}"}}]}}],
                                    "usage":{"prompt_tokens":50,"completion_tokens":20}}
                                    """)
                            .withStatus(200))
                    .willSetStateTo("tool-failed"));

            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .inScenario("error-tool")
                    .whenScenarioStateIs("tool-failed")
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"choices":[{"finish_reason":"stop","message":{"role":"assistant",
                                    "content":"抱歉，工具执行失败，请稍后重试。"}}],
                                    "usage":{"prompt_tokens":100,"completion_tokens":30}}
                                    """)
                            .withStatus(200)));

            List<ToolDefinition> tools = List.of(ToolDefinition.of(
                    "failingTool", "失败的", ToolDefinition.Parameters.builder().build()));

            com.example.model.ToolExecutor executor = (id, name, args) -> {
                throw new RuntimeException("模拟工具异常");
            };

            FunctionCallResult result = deepSeekService.functionCall(
                    "测试", "call", tools, executor, 2);

            assertThat(result).isNotNull();
            assertThat(result.getToolCalls()).hasSize(1);
            assertThat(result.getContent()).contains("工具执行失败");
        }

        @Test
        @DisplayName("超过最大轮数应抛 BusinessException")
        void shouldThrowWhenExceedMaxRounds() {
            for (int i = 0; i < 10; i++) {
                stubFor(post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {"choices":[{"finish_reason":"tool_calls","message":{
                                        "tool_calls":[{"id":"loop","type":"function",
                                        "function":{"name":"loop","arguments":"{}"}}]}}],
                                        "usage":{"prompt_tokens":10,"completion_tokens":10}}
                                        """)
                                .withStatus(200)));
            }

            List<ToolDefinition> tools = List.of(ToolDefinition.of(
                    "loop", "循环", ToolDefinition.Parameters.builder().build()));
            com.example.model.ToolExecutor executor = (id, name, args) -> "ok";

            try {
                deepSeekService.functionCall("t", "x", tools, executor, 3);
                assertThat(false).as("应该抛出异常").isTrue();
            } catch (com.example.common.BusinessException e) {
                assertThat(e.getMessage()).contains("超过最大轮数");
            }
        }
    }

    // ==================== 健康领域场景 ====================

    @Nested
    @DisplayName("健康领域工具调用")
    class HealthDomain {

        @Test
        @DisplayName("并行工具调用格式解析（运动热量 + BMR）")
        void shouldParseParallelToolCalls() throws Exception {
            String json = """
                    {
                      "tool_calls": [
                        {"id":"c1","type":"function","function":{
                          "name":"calculateExerciseCalories",
                          "arguments":"{\\"exerciseName\\":\\"跑步\\",\\"durationMinutes\\":30}"}},
                        {"id":"c2","type":"function","function":{
                          "name":"calculateBmr",
                          "arguments":"{\\"height\\":175,\\"weight\\":70,\\"age\\":30,\\"gender\\":\\"male\\"}"}}
                      ]
                    }
                    """;

            JsonNode root = objectMapper.readTree(json);
            var calls = deepSeekService.extractToolCalls(root.path("tool_calls"));

            assertThat(calls).hasSize(2);
            assertThat(calls.get(0).getName()).isEqualTo("calculateExerciseCalories");
            assertThat(calls.get(1).getName()).isEqualTo("calculateBmr");
            assertThat(calls.get(0).getArgumentsNode().path("durationMinutes").asInt()).isEqualTo(30);
            assertThat(calls.get(1).getArgumentsNode().path("weight").asInt()).isEqualTo(70);
        }

        @Test
        @DisplayName("参数枚举约束序列化兼容 DeepSeek")
        void shouldSerializeEnumConstraints() {
            ToolDefinition.PropertyDef goalProp = ToolDefinition.PropertyDef.builder()
                    .type("string")
                    .description("健康目标")
                    .enumValues(List.of("lose_weight", "gain_muscle", "maintain"))
                    .build();

            Map<String, Object> map = goalProp.toMap();
            assertThat(map.get("type")).isEqualTo("string");

            @SuppressWarnings("unchecked")
            List<String> enums = (List<String>) map.get("enum");
            assertThat(enums).containsExactly("lose_weight", "gain_muscle", "maintain");
        }

        @Test
        @DisplayName("完整 JSON Schema 序列化：运动类型 + 心率区间工具")
        void shouldSerializeCompleteHealthToolSchema() {
            // calculateExerciseCalories 的 JSON Schema
            ToolDefinition.Parameters params = ToolDefinition.Parameters.builder()
                    .property("userId", ToolDefinition.PropertyDef.builder()
                            .type("integer").description("用户ID").build())
                    .property("exerciseName", ToolDefinition.PropertyDef.builder()
                            .type("string").description("运动项目名称").build())
                    .property("durationMinutes", ToolDefinition.PropertyDef.builder()
                            .type("integer").description("运动时长（分钟）").build())
                    .required(List.of("userId", "exerciseName", "durationMinutes"))
                    .build();

            ToolDefinition tool = ToolDefinition.of(
                    "calculateExerciseCalories",
                    "根据运动类型、时长和用户体重计算消耗热量（MET法）",
                    params);

            List<Map<String, Object>> built = deepSeekService.buildToolDefinitions(List.of(tool));
            assertThat(built).hasSize(1);

            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) built.get(0).get("function");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) func.get("parameters");
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) parameters.get("properties");

            assertThat(props).containsKeys("userId", "exerciseName", "durationMinutes");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) parameters.get("required");
            assertThat(required).containsExactly("userId", "exerciseName", "durationMinutes");

            // 验证每个 property 的 type 字段
            @SuppressWarnings("unchecked")
            Map<String, Object> userIdProp = (Map<String, Object>) props.get("userId");
            assertThat(userIdProp.get("type")).isEqualTo("integer");
            assertThat(userIdProp.get("description")).isEqualTo("用户ID");
        }
    }
}