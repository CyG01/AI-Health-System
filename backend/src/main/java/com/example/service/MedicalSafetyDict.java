package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 医疗安全词典 — 医疗红线 Layer1 正则拦截。
 *
 * 包含三类禁止词汇，编译为预编译正则模式，用于在 AI 调用前/后执行硬匹配拦截。
 * 所有词汇均可通过管理后台扩展（数据库规则表），此类为内置基准词库。
 *
 * 三类词库：
 * 1. 处方药名（100+）— AI 不得推荐任何处方药物
 * 2. 疾病诊断术语（50+）— AI 不得给出诊断性结论
 * 3. 剂量相关词汇 — AI 不得给出具体剂量建议
 */
@Slf4j
@Component
public class MedicalSafetyDict {

    // ==================== 1. 处方药名（100+） ====================
    // 覆盖降糖药、降压药、降脂药、抗生素、激素、精神类、抗癌药等
    private static final Set<String> PRESCRIPTION_DRUGS = Set.of(
            // 降糖药
            "二甲双胍", "格列本脲", "格列齐特", "格列美脲", "格列吡嗪",
            "瑞格列奈", "那格列奈", "吡格列酮", "罗格列酮", "西格列汀",
            "沙格列汀", "维格列汀", "利拉鲁肽", "艾塞那肽", "达格列净",
            "恩格列净", "卡格列净", "阿卡波糖", "伏格列波糖", "米格列醇",
            "胰岛素", "甘精胰岛素", "门冬胰岛素", "地特胰岛素", "赖脯胰岛素",
            // 降压药
            "硝苯地平", "氨氯地平", "非洛地平", "尼群地平", "拉西地平",
            "卡托普利", "依那普利", "贝那普利", "培哚普利", "赖诺普利",
            "氯沙坦", "缬沙坦", "厄贝沙坦", "替米沙坦", "坎地沙坦",
            "美托洛尔", "比索洛尔", "阿替洛尔", "普萘洛尔", "卡维地洛",
            "氢氯噻嗪", "呋塞米", "螺内酯", "吲达帕胺", "多沙唑嗪",
            // 降脂药
            "阿托伐他汀", "瑞舒伐他汀", "辛伐他汀", "普伐他汀", "氟伐他汀",
            "非诺贝特", "苯扎贝特", "吉非罗齐", "依折麦布", "普罗布考",
            // 抗生素
            "阿莫西林", "头孢克洛", "头孢呋辛", "头孢克肟", "头孢曲松",
            "左氧氟沙星", "莫西沙星", "环丙沙星", "阿奇霉素", "克拉霉素",
            "罗红霉素", "多西环素", "米诺环素", "甲硝唑", "替硝唑",
            // 激素类
            "泼尼松", "地塞米松", "甲泼尼龙", "氢化可的松", "倍他米松",
            "黄体酮", "雌二醇", "左甲状腺素钠", "甲巯咪唑", "丙硫氧嘧啶",
            // 精神/神经系统
            "舍曲林", "氟西汀", "帕罗西汀", "艾司西酞普兰", "文拉法辛",
            "阿普唑仑", "艾司唑仑", "地西泮", "氯硝西泮", "劳拉西泮",
            "奥氮平", "利培酮", "喹硫平", "阿立哌唑", "碳酸锂",
            "加巴喷丁", "普瑞巴林", "卡马西平", "丙戊酸钠", "托吡酯",
            // 抗癌/免疫抑制剂
            "甲氨蝶呤", "环磷酰胺", "他莫昔芬", "来曲唑", "伊马替尼",
            "环孢素", "他克莫司", "霉酚酸酯", "硫唑嘌呤", "羟氯喹"
    );

    // ==================== 2. 疾病诊断术语（50+） ====================
    // AI 不得下诊断性结论
    private static final Set<String> DIAGNOSIS_TERMS = Set.of(
            // 心血管
            "冠心病", "急性心肌梗死", "心力衰竭", "心律失常", "心房颤动",
            "心肌缺血", "心绞痛", "心源性休克", "心脏骤停", "主动脉夹层",
            // 脑血管
            "脑梗死", "脑出血", "短暂性脑缺血发作", "脑卒中", "脑动脉瘤",
            // 肿瘤
            "恶性肿瘤", "肺癌", "肝癌", "胃癌", "乳腺癌",
            "结肠癌", "直肠癌", "胰腺癌", "淋巴瘤", "白血病",
            // 内分泌
            "糖尿病酮症酸中毒", "甲亢危象", "肾上腺皮质功能减退",
            // 呼吸
            "慢性阻塞性肺疾病", "呼吸衰竭", "肺栓塞", "气胸",
            // 消化
            "消化道出血", "急性胰腺炎", "肠梗阻", "肝硬化失代偿",
            // 泌尿
            "急性肾衰竭", "慢性肾功能不全", "尿毒症",
            // 骨骼/免疫
            "类风湿关节炎", "系统性红斑狼疮", "强直性脊柱炎",
            // 精神
            "抑郁症", "焦虑症", "精神分裂症", "双相情感障碍",
            // 感染
            "败血症", "感染性休克", "结核病", "HIV/AIDS",
            // 其他诊断性表述
            "确诊", "明确诊断", "临床诊断", "病理诊断", "影像学诊断"
    );

    // ==================== 3. 剂量相关词汇 ====================
    // AI 不得给出任何具体药物剂量建议
    private static final Set<String> DOSE_TERMS = Set.of(
            "mg", "毫克", "μg", "微克", "g", "ml", "毫升",
            "每日剂量", "每次剂量", "给药剂量", "起始剂量", "维持剂量",
            "负荷剂量", "递增剂量", "最大剂量", "最小有效剂量",
            "剂型", "规格", "用法用量", "一日三次", "一日两次",
            "tid", "bid", "qd", "qid", "prn",
            "遵医嘱", "按医嘱", "遵处方"
    );

    // ==================== 预编译正则 ====================

    /** 处方药名词边界匹配模式 */
    private final Pattern drugPattern;

    /** 疾病诊断术语边界匹配模式 */
    private final Pattern diagnosisPattern;

    /** 剂量词汇边界匹配模式 */
    private final Pattern dosePattern;

    /** 组合匹配模式（用于快速扫描） */
    private final Pattern combinedPattern;

    public MedicalSafetyDict() {
        this.drugPattern = buildPattern(PRESCRIPTION_DRUGS);
        this.diagnosisPattern = buildPattern(DIAGNOSIS_TERMS);
        this.dosePattern = buildPattern(DOSE_TERMS);
        this.combinedPattern = buildCombinedPattern();
        log.info("医疗安全词典加载完成: 处方药={} 诊断术语={} 剂量词={}",
                PRESCRIPTION_DRUGS.size(), DIAGNOSIS_TERMS.size(), DOSE_TERMS.size());
    }

    // ==================== 公共检查接口 ====================

    /**
     * 对用户输入文本执行医疗红线检查。
     *
     * @param text 待检查文本（用户输入或 AI 输出）
     * @return 检查结果（是否通过 + 具体匹配项列表）
     */
    public MedicalSafetyResult check(String text) {
        if (text == null || text.isBlank()) {
            return MedicalSafetyResult.pass();
        }

        List<MedicalMatch> matches = new ArrayList<>();

        // 快速预扫描：无任何匹配则直接跳过
        if (!combinedPattern.matcher(text).find()) {
            return MedicalSafetyResult.pass();
        }

        // 精准匹配处方药名
        var drugMatcher = drugPattern.matcher(text);
        while (drugMatcher.find()) {
            matches.add(new MedicalMatch("PRESCRIPTION_DRUG", drugMatcher.group(), drugMatcher.start()));
        }

        // 精准匹配诊断术语
        var diagMatcher = diagnosisPattern.matcher(text);
        while (diagMatcher.find()) {
            matches.add(new MedicalMatch("DIAGNOSIS_TERM", diagMatcher.group(), diagMatcher.start()));
        }

        // 精准匹配剂量词汇
        var doseMatcher = dosePattern.matcher(text);
        while (doseMatcher.find()) {
            matches.add(new MedicalMatch("DOSE_TERM", doseMatcher.group(), doseMatcher.start()));
        }

        if (matches.isEmpty()) {
            return MedicalSafetyResult.pass();
        }

        return MedicalSafetyResult.block(matches);
    }

    // ==================== 辅助方法 ====================

    private Pattern buildPattern(Set<String> terms) {
        String regex = terms.stream()
                .sorted(Comparator.comparingInt(String::length).reversed()) // 长词优先
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    private Pattern buildCombinedPattern() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(PRESCRIPTION_DRUGS);
        all.addAll(DIAGNOSIS_TERMS);
        all.addAll(DOSE_TERMS);
        String regex = all.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    }

    // ==================== 内部类 ====================

    /**
     * 医疗安全检查结果。
     */
    public static class MedicalSafetyResult {
        private final boolean passed;
        private final List<MedicalMatch> matches;

        private MedicalSafetyResult(boolean passed, List<MedicalMatch> matches) {
            this.passed = passed;
            this.matches = matches;
        }

        public static MedicalSafetyResult pass() {
            return new MedicalSafetyResult(true, List.of());
        }

        public static MedicalSafetyResult block(List<MedicalMatch> matches) {
            return new MedicalSafetyResult(false, matches);
        }

        public boolean isPassed() { return passed; }
        public List<MedicalMatch> getMatches() { return matches; }
    }

    /**
     * 单个匹配记录。
     */
    public static class MedicalMatch {
        private final String category;   // PRESCRIPTION_DRUG / DIAGNOSIS_TERM / DOSE_TERM
        private final String keyword;    // 匹配到的词汇
        private final int position;       // 在文本中的位置

        public MedicalMatch(String category, String keyword, int position) {
            this.category = category;
            this.keyword = keyword;
            this.position = position;
        }

        public String getCategory() { return category; }
        public String getKeyword() { return keyword; }
        public int getPosition() { return position; }
    }
}