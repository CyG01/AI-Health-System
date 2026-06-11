package com.example.sdui;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一 AI 响应结构。
 * 所有 AI 接口返回此结构，前端根据协议版本和 Widget 类型动态渲染。
 * 旧客户端降级展示 text 字段纯文本内容。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentResponse {

    /** SDUI 协议版本，用于前端判断兼容性 */
    @Builder.Default
    private String protocolVersion = "1.0";

    /** 要求的最低客户端版本列表，如 [">=1.2.0"] */
    @Builder.Default
    private List<String> requiredClientVersions = new ArrayList<>();

    /** 纯文本兜底说明（旧客户端只渲染此字段） */
    private String text;

    /** 动态渲染组件列表 */
    @Builder.Default
    private List<Widget> widgets = new ArrayList<>();

    /** 已执行的 Tool 调用结果 */
    @Builder.Default
    private List<ToolCallResult> toolCalls = new ArrayList<>();

    /** 医疗免责声明 */
    private String disclaimer;

    /** 引用的知识来源 */
    @Builder.Default
    private List<String> knowledgeSources = new ArrayList<>();

    /** 扩展元数据 */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 快捷构造：仅包含纯文本的响应（兼容旧客户端）。
     */
    public static AiAgentResponse textOnly(String text) {
        return AiAgentResponse.builder()
                .text(text)
                .disclaimer("本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。")
                .build();
    }

    /**
     * 快捷构造：包含纯文本 + Widget 组件的响应。
     */
    public static AiAgentResponse withWidgets(String text, List<Widget> widgets) {
        return AiAgentResponse.builder()
                .text(text)
                .widgets(widgets)
                .disclaimer("本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。")
                .build();
    }
}