package com.example.util;

import org.springframework.stereotype.Component;

/**
 * 医疗免责过滤器
 * 在所有 AI 输出的健康建议/计划/聊天回复末尾自动追加医疗免责声明
 *
 * 法规依据：《互联网诊疗管理办法》《个人信息保护法》
 * AI 生成的健康建议不属于医疗行为，必须在显著位置标示免责声明
 */
@Component
public class MedicalDisclaimerFilter {

    private static final String DISCLAIMER =
            "\n\n---\n\uD83D\uDEAB *本建议由AI生成，仅供参考，不构成医疗诊断或处方。" +
            "如有健康问题，请及时咨询专业医生。*";

    /**
     * 对 AI 输出内容追加医疗免责声明
     */
    public String appendDisclaimer(String aiContent) {
        if (aiContent == null || aiContent.isBlank()) {
            return aiContent;
        }
        // 如果已存在类似免责声明，不重复追加
        if (aiContent.contains("不构成医疗诊断") || aiContent.contains("咨询专业医生")) {
            return aiContent;
        }
        return aiContent + DISCLAIMER;
    }
}