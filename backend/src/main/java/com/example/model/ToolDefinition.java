package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Function Calling 工具定义，兼容 DeepSeek / OpenAI 的 tools 参数格式。
 * <p>
 * DeepSeek API 完全兼容 OpenAI 的 Function Calling 协议（tools 参数），
 * 支持 parallel_tool_calls 和 strict 模式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /** 工具类型，固定为 "function" */
    @Builder.Default
    private String type = "function";

    /** 函数定义 */
    private FunctionDef function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionDef {
        /** 函数名（DeepSeek 用于匹配 tool_choice） */
        private String name;

        /** 函数描述（帮助模型判断何时调用） */
        private String description;

        /** JSON Schema 参数定义 */
        private Parameters parameters;

        /**
         * 是否启用 strict 模式（DeepSeek 支持）。
         * strict=true 时模型只生成 JSON Schema 定义的字段。
         */
        @Builder.Default
        private boolean strict = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameters {
        /** 固定为 "object" */
        @Builder.Default
        private String type = "object";

        /** JSON Schema 属性 */
        @Singular
        private Map<String, PropertyDef> properties;

        /** 必填字段列表 */
        @Singular("requiredField")
        private List<String> required;

        /** 是否允许额外属性 */
        @Builder.Default
        private boolean additionalProperties = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyDef {
        /** 参数类型：string / number / integer / boolean / array / object */
        private String type;

        /** 参数描述 */
        private String description;

        /** 枚举值约束（string 类型） */
        private List<String> enumValues;

        /**
         * 构造 map 时需要将 enumValues 映射为 "enum" 键。
         * Jackson 序列化时用 @JsonProperty("enum") 会有保留字问题，这里手工处理。
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            if (description != null) {
                map.put("description", description);
            }
            if (enumValues != null && !enumValues.isEmpty()) {
                map.put("enum", enumValues);
            }
            return map;
        }
    }

    /**
     * 快捷构造：简单工具定义。
     */
    public static ToolDefinition of(String name, String description, Parameters params) {
        return ToolDefinition.builder()
                .function(FunctionDef.builder()
                        .name(name)
                        .description(description)
                        .parameters(params)
                        .build())
                .build();
    }
}