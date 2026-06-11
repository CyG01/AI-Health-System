package com.example.util;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务 —— 合规刚需
 * 确保敏感信息在传给第三方大模型前完成泛化/脱敏处理
 *
 * 数据分级：
 * L1 公开级 —— 运动类型、食物名称 → 直接上传
 * L2 一般级 —— 身高、体重、BMI → 直接上传
 * L3 敏感级 —— 疾病史、过敏史、用药史、家族病史 → 泛化后上传
 * L4 极度敏感 —— 身份证、手机号、基因数据 → 禁止上传
 */
@Service
public class DataMaskingService {

    // ======================== L4 极度敏感 ========================

    /** L4 字段名：包含任一字段的请求应被拦截 */
    private static final Set<String> L4_FIELDS = Set.of(
            "idCard", "realName", "phone", "geneticData", "idNumber",
            "password", "creditCard", "socialSecurity", "passport"
    );

    /** L4 中文关键词：在文本中检测到后移除 */
    private static final String[] L4_PATTERNS = {
            "身份证", "手机号", "电话号码", "银行卡", "社保号", "护照号", "驾驶证号"
    };

    /** 预编译 L4 正则，匹配 "关键词[:：]值" 格式 */
    private static final Pattern L4_KV_PATTERN = Pattern.compile(
            "(身份证|手机号|电话号码|银行卡|社保号|护照号|驾驶证号)\\s*[:：]\\s*[^,;，；\\n]+"
    );

    // ======================== L3 疾病泛化 ========================

    private static final Map<String, String> DISEASE_ALIAS = Map.ofEntries(
            // 免疫系统
            Map.entry("HIV", "严重免疫系统疾病"),
            Map.entry("艾滋病", "严重免疫系统疾病"),
            Map.entry("AIDS", "严重免疫系统疾病"),
            Map.entry("红斑狼疮", "自身免疫性疾病"),
            Map.entry("系统性红斑狼疮", "自身免疫性疾病"),
            Map.entry("SLE", "自身免疫性疾病"),
            // 精神/神经系统
            Map.entry("精神分裂症", "神经系统疾病"),
            Map.entry("精神分裂", "神经系统疾病"),
            Map.entry("癫痫", "神经系统疾病"),
            Map.entry("帕金森", "神经系统疾病"),
            Map.entry("阿尔茨海默", "神经系统疾病"),
            Map.entry("老年痴呆", "神经系统疾病"),
            // 肿瘤
            Map.entry("癌症", "慢性重大疾病"),
            Map.entry("恶性肿瘤", "慢性重大疾病"),
            Map.entry("白血病", "血液系统疾病"),
            Map.entry("淋巴瘤", "血液系统疾病"),
            Map.entry("骨髓瘤", "血液系统疾病"),
            // 肝脏
            Map.entry("乙肝", "慢性肝脏疾病"),
            Map.entry("丙肝", "慢性肝脏疾病"),
            Map.entry("甲肝", "病毒性肝脏疾病"),
            Map.entry("肝硬化", "慢性肝脏疾病"),
            Map.entry("肝癌", "慢性肝脏疾病"),
            Map.entry("脂肪肝", "肝脏代谢异常"),
            // 性传播疾病
            Map.entry("梅毒", "感染性疾病"),
            Map.entry("淋病", "感染性疾病"),
            Map.entry("尖锐湿疣", "感染性疾病"),
            Map.entry("生殖器疱疹", "感染性疾病"),
            // 呼吸系统
            Map.entry("肺结核", "慢性呼吸系统疾病"),
            Map.entry("肺纤维化", "慢性呼吸系统疾病"),
            Map.entry("慢阻肺", "慢性呼吸系统疾病"),
            Map.entry("COPD", "慢性呼吸系统疾病"),
            // 肾脏
            Map.entry("尿毒症", "严重肾脏疾病"),
            Map.entry("肾衰竭", "严重肾脏疾病"),
            Map.entry("慢性肾炎", "慢性肾脏疾病"),
            Map.entry("肾病综合征", "慢性肾脏疾病"),
            // 血液/遗传
            Map.entry("血友病", "凝血功能障碍"),
            Map.entry("地中海贫血", "遗传性血液疾病"),
            Map.entry("镰刀型细胞贫血", "遗传性血液疾病"),
            // 心血管
            Map.entry("先天性心脏病", "心血管系统疾病"),
            Map.entry("冠心病", "心血管系统疾病"),
            Map.entry("心肌梗死", "心血管系统疾病"),
            // 内分泌
            Map.entry("糖尿病", "代谢性疾病"),
            Map.entry("甲亢", "内分泌系统疾病"),
            Map.entry("甲减", "内分泌系统疾病"),
            Map.entry("甲状腺功能亢进", "内分泌系统疾病"),
            Map.entry("甲状腺功能减退", "内分泌系统疾病")
    );

    // ======================== L3 用药泛化 ========================

    private static final Map<String, String> MEDICATION_ALIAS = Map.ofEntries(
            // 降糖类
            Map.entry("胰岛素", "降糖药物"),
            Map.entry("二甲双胍", "降糖药物"),
            Map.entry("格列美脲", "降糖药物"),
            Map.entry("阿卡波糖", "降糖药物"),
            // 降压类
            Map.entry("硝苯地平", "降压药物"),
            Map.entry("氨氯地平", "降压药物"),
            Map.entry("缬沙坦", "降压药物"),
            Map.entry("厄贝沙坦", "降压药物"),
            Map.entry("卡托普利", "降压药物"),
            Map.entry("美托洛尔", "心血管药物"),
            // 降脂类
            Map.entry("阿托伐他汀", "降脂药物"),
            Map.entry("瑞舒伐他汀", "降脂药物"),
            Map.entry("辛伐他汀", "降脂药物"),
            // 抗肿瘤
            Map.entry("化疗", "抗肿瘤治疗"),
            Map.entry("放疗", "抗肿瘤治疗"),
            Map.entry("靶向药", "抗肿瘤治疗"),
            Map.entry("他莫昔芬", "抗肿瘤药物"),
            // 抗凝
            Map.entry("华法林", "抗凝药物"),
            Map.entry("阿司匹林", "抗凝药物"),
            Map.entry("氯吡格雷", "抗凝药物"),
            // 精神类
            Map.entry("奥氮平", "神经系统药物"),
            Map.entry("利培酮", "神经系统药物"),
            Map.entry("舍曲林", "神经系统药物"),
            Map.entry("氟西汀", "神经系统药物"),
            Map.entry("帕罗西汀", "神经系统药物"),
            Map.entry("阿普唑仑", "镇静类药物"),
            Map.entry("艾司唑仑", "镇静类药物"),
            Map.entry("地西泮", "镇静类药物"),
            // 免疫抑制
            Map.entry("环孢素", "免疫抑制剂"),
            Map.entry("他克莫司", "免疫抑制剂"),
            Map.entry("甲氨蝶呤", "免疫抑制剂"),
            // 激素类
            Map.entry("泼尼松", "激素类药物"),
            Map.entry("地塞米松", "激素类药物"),
            Map.entry("甲泼尼龙", "激素类药物")
    );

    // ======================== L3 过敏泛化 ========================

    private static final Map<String, String> ALLERGY_ALIAS = Map.ofEntries(
            // 抗生素类
            Map.entry("青霉素", "抗生素类"),
            Map.entry("头孢", "抗生素类"),
            Map.entry("阿莫西林", "抗生素类"),
            Map.entry("氨苄西林", "抗生素类"),
            Map.entry("红霉素", "抗生素类"),
            Map.entry("四环素", "抗生素类"),
            Map.entry("氯霉素", "抗生素类"),
            Map.entry("庆大霉素", "抗生素类"),
            Map.entry("磺胺", "抗生素类"),
            // 解热镇痛
            Map.entry("布洛芬", "解热镇痛药物"),
            Map.entry("对乙酰氨基酚", "解热镇痛药物"),
            Map.entry("扑热息痛", "解热镇痛药物"),
            // 海鲜/食物
            Map.entry("虾", "甲壳类食物"),
            Map.entry("蟹", "甲壳类食物"),
            Map.entry("贝类", "甲壳类食物"),
            Map.entry("花生", "坚果类食物"),
            Map.entry("核桃", "坚果类食物"),
            Map.entry("杏仁", "坚果类食物"),
            Map.entry("芒果", "热带水果"),
            Map.entry("菠萝", "热带水果"),
            Map.entry("猕猴桃", "热带水果"),
            Map.entry("牛奶", "乳制品"),
            Map.entry("鸡蛋", "禽蛋类"),
            // 环境
            Map.entry("花粉", "季节性过敏原"),
            Map.entry("尘螨", "室内过敏原"),
            Map.entry("霉菌", "环境过敏原"),
            Map.entry("猫毛", "宠物皮屑"),
            Map.entry("狗毛", "宠物皮屑")
    );

    // ======================== 公共方法 ========================

    /**
     * 检查给定字段名是否为 L4 极度敏感字段
     */
    public boolean isL4Field(String fieldName) {
        if (fieldName == null) return false;
        return L4_FIELDS.contains(fieldName);
    }

    /**
     * 对任意文本进行 L4 关键词拦截检测
     * @return true 表示文本包含 L4 敏感信息
     */
    public boolean containsL4Content(String text) {
        if (text == null || text.isBlank()) return false;
        for (String pattern : L4_PATTERNS) {
            if (text.contains(pattern)) return true;
        }
        return false;
    }

    /**
     * 对疾病史进行泛化脱敏
     * 将敏感疾病名称替换为泛化描述，移除 L4 信息
     */
    public String maskDiseaseHistory(String history) {
        if (history == null || history.isBlank()) {
            return "";
        }
        String masked = applyDiseaseAlias(history);
        // 移除 L4 关键词及后续值
        masked = L4_KV_PATTERN.matcher(masked).replaceAll("$1:[已隐藏]");
        // 清理残留的 L4 关键词裸文本
        for (String pattern : L4_PATTERNS) {
            masked = masked.replace(pattern, "");
        }
        // 清理多余空格
        masked = masked.replaceAll("\\s{2,}", " ").trim();
        if (masked.length() > 500) {
            masked = masked.substring(0, 500) + "...";
        }
        return masked;
    }

    /**
     * 对家族病史进行泛化脱敏（复用疾病别名映射）
     */
    public String maskFamilyHistory(String familyHistory) {
        if (familyHistory == null || familyHistory.isBlank()) {
            return "";
        }
        String masked = applyDiseaseAlias(familyHistory);
        masked = L4_KV_PATTERN.matcher(masked).replaceAll("$1:[已隐藏]");
        for (String pattern : L4_PATTERNS) {
            masked = masked.replace(pattern, "");
        }
        masked = masked.replaceAll("\\s{2,}", " ").trim();
        if (masked.length() > 300) {
            masked = masked.substring(0, 300) + "...";
        }
        return masked;
    }

    /**
     * 过敏史脱敏 — 将具体过敏原泛化为大类
     * 保留过敏信息对 AI 的参考价值，同时降低可识别性
     */
    public String maskAllergyHistory(String allergy) {
        if (allergy == null || allergy.isBlank()) {
            return "";
        }
        String masked = allergy;
        for (Map.Entry<String, String> entry : ALLERGY_ALIAS.entrySet()) {
            masked = masked.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        // 移除 L4 关键词
        for (String pattern : L4_PATTERNS) {
            masked = masked.replace(pattern, "");
        }
        masked = masked.replaceAll("\\s{2,}", " ").trim();
        if (masked.length() > 200) {
            masked = masked.substring(0, 200) + "...";
        }
        return masked;
    }

    /**
     * 当前用药脱敏 — 将具体药名泛化为药物大类
     */
    public String maskMedication(String medication) {
        if (medication == null || medication.isBlank()) {
            return "";
        }
        String masked = medication;
        for (Map.Entry<String, String> entry : MEDICATION_ALIAS.entrySet()) {
            masked = masked.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        // 移除 L4 关键词
        masked = L4_KV_PATTERN.matcher(masked).replaceAll("$1:[已隐藏]");
        for (String pattern : L4_PATTERNS) {
            masked = masked.replace(pattern, "");
        }
        masked = masked.replaceAll("\\s{2,}", " ").trim();
        if (masked.length() > 300) {
            masked = masked.substring(0, 300) + "...";
        }
        return masked;
    }

    /**
     * 用户名脱敏
     */
    public String maskUserName(String name) {
        if (name == null || name.length() <= 1) {
            return "用户";
        }
        return name.charAt(0) + "**";
    }

    /**
     * 用户画像脱敏（供 DeepSeekService 使用的综合方法）
     * 将 healthRecord 拼接的 userProfile 进行脱敏
     */
    public String maskUserProfile(String userProfile) {
        if (userProfile == null || userProfile.isBlank()) {
            return "";
        }
        String masked = userProfile;
        // L4 字段：替换 "关键词:值" 为 "关键词:[已隐藏]"
        masked = L4_KV_PATTERN.matcher(masked).replaceAll("$1:[已隐藏]");
        // L4 裸关键词（可能未带冒号）
        for (String pattern : L4_PATTERNS) {
            masked = masked.replace(pattern, "");
        }
        // L3 疾病泛化
        masked = applyDiseaseAlias(masked);
        // L3 用药泛化
        for (Map.Entry<String, String> entry : MEDICATION_ALIAS.entrySet()) {
            masked = masked.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        // L3 过敏泛化
        for (Map.Entry<String, String> entry : ALLERGY_ALIAS.entrySet()) {
            masked = masked.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        // 清理多余空格
        masked = masked.replaceAll("\\s{2,}", " ").trim();
        return masked;
    }

    /**
     * 对 health goal 进行注入过滤+脱敏
     */
    public String maskGoal(String goal) {
        if (goal == null || goal.isBlank()) {
            return "保持健康";
        }
        String result = PromptSanitizer.sanitize(goal);
        if (result.length() > 100) {
            result = result.substring(0, 100);
        }
        return result;
    }

    // ======================== 私有辅助 ========================

    /**
     * 应用疾病别名映射（公共逻辑，供 diseaseHistory / familyHistory 复用）
     */
    private String applyDiseaseAlias(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : DISEASE_ALIAS.entrySet()) {
            result = result.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }
        return result;
    }
}