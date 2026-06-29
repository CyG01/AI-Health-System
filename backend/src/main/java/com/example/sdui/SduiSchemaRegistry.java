package com.example.sdui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SDUI JSON Schema 注册中心 — 标准化全部 13 种 Widget 的 JSON Schema。
 * 供 Web 端消费，确保渲染层行为一致。
 *
 * 协议版本：2.0
 * 支持的 13 种 Widget 类型：
 *   text_block / tip / stat_card / comparison / progress_ring / timer
 *   / notification / quiz / exercise_card / exercise_phase / meal_chart
 *   / meal_plan / sleep_chart
 */
public final class SduiSchemaRegistry {

    /** 当前 SDUI 协议版本 */
    public static final String PROTOCOL_VERSION = "2.0";

    /** 最低客户端版本要求 */
    public static final List<String> REQUIRED_CLIENT_VERSIONS = List.of(">=1.3.0");

    /** 所有已注册的 Widget 类型 */
    private static final Set<String> REGISTERED_TYPES = new LinkedHashSet<>(Arrays.asList(
            "text_block", "tip", "stat_card", "comparison", "progress_ring",
            "timer", "notification", "quiz", "exercise_card", "exercise_phase",
            "meal_chart", "meal_plan", "sleep_chart"
    ));

    /** Widget type → JSON Schema 缓存 */
    private static final Map<String, ObjectNode> SCHEMA_CACHE = new ConcurrentHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SduiSchemaRegistry() {}

    /**
     * 返回全部 13 种 Widget 的 JSON Schema 列表。
     */
    public static List<ObjectNode> getAllSchemas() {
        List<ObjectNode> schemas = new ArrayList<>();
        for (String type : REGISTERED_TYPES) {
            schemas.add(getSchema(type));
        }
        return schemas;
    }

    /**
     * 按类型获取 Widget 的 JSON Schema（带缓存）。
     */
    public static ObjectNode getSchema(String widgetType) {
        return SCHEMA_CACHE.computeIfAbsent(widgetType, SduiSchemaRegistry::buildSchema);
    }

    /**
     * 验证 Widget 的类型是否已注册。
     */
    public static boolean isRegistered(String widgetType) {
        return REGISTERED_TYPES.contains(widgetType);
    }

    /**
     * 返回所有已注册的 Widget 类型。
     */
    public static Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(REGISTERED_TYPES);
    }

    /**
     * 序列化 SDUI 协议元信息 + 全部 Schema 列表。
     */
    public static String toProtocolManifestJson() throws JsonProcessingException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("protocolVersion", PROTOCOL_VERSION);
        root.set("requiredClientVersions", MAPPER.valueToTree(REQUIRED_CLIENT_VERSIONS));

        ArrayNode schemas = MAPPER.createArrayNode();
        for (String type : REGISTERED_TYPES) {
            schemas.add(getSchema(type));
        }
        root.set("widgets", schemas);
        root.put("widgetCount", REGISTERED_TYPES.size());

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    // ======================== Schema 构建 ========================

    private static ObjectNode buildSchema(String type) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        node.put("widgetId", "ai-health." + type);
        node.put("title", getWidgetTitle(type));
        node.put("description", getWidgetDescription(type));
        node.put("version", "2.0");
        node.put("type", "object");
        node.set("required", MAPPER.valueToTree(getRequiredFields(type)));
        node.set("properties", buildProperties(type));
        node.set("actions", buildActions(type));
        node.set("degradations", buildDegradations(type));
        return node;
    }

    private static String getWidgetTitle(String type) {
        return switch (type) {
            case "text_block"      -> "文本块";
            case "tip"             -> "提示条";
            case "stat_card"       -> "统计卡片";
            case "comparison"      -> "前后对比";
            case "progress_ring"   -> "进度环";
            case "timer"           -> "计时器";
            case "notification"    -> "通知提醒";
            case "quiz"            -> "健康问答";
            case "exercise_card"   -> "运动卡片";
            case "exercise_phase"  -> "运动阶段";
            case "meal_chart"      -> "饮食图表";
            case "meal_plan"       -> "餐饮计划";
            case "sleep_chart"     -> "睡眠图表";
            default                -> type;
        };
    }

    private static String getWidgetDescription(String type) {
        return switch (type) {
            case "text_block"      -> "纯文本内容展示组件，作为降级兜底。支持 Markdown 格式。";
            case "tip"             -> "简短健康提示/建议标签，支持图标和分类。";
            case "stat_card"       -> "展示关键指标数值、单位和变化趋势。";
            case "comparison"      -> "前后数据对比（体重变化、指标对比等），显示变化百分比。";
            case "progress_ring"   -> "环形进度条，展示目标完成度百分比。";
            case "timer"           -> "运动/休息计时器，支持开始、暂停、重置操作。";
            case "notification"    -> "系统通知或AI建议推送，支持跳转操作。";
            case "quiz"            -> "交互式健康知识问答，支持答案验证和解释。";
            case "exercise_card"   -> "展示单项运动任务详情，支持打卡、视频、阶段拆分。";
            case "exercise_phase"  -> "运动结构化阶段拆分（热身/核心/放松），逐阶段打卡。";
            case "meal_chart"      -> "热量和营养素摄入与剩余情况图表展示。";
            case "meal_plan"       -> "一日三餐推荐，含食物条目和营养素分配。";
            case "sleep_chart"     -> "睡眠质量评分、各阶段时长和改善建议。";
            default                -> "未知组件类型，前端应降级为纯文本展示。";
        };
    }

    private static List<String> getRequiredFields(String type) {
        return switch (type) {
            case "text_block"      -> List.of("type", "content");
            case "tip"             -> List.of("type", "content");
            case "stat_card"       -> List.of("type", "metricName", "value");
            case "comparison"      -> List.of("type", "beforeValue", "afterValue");
            case "progress_ring"   -> List.of("type", "percentage");
            case "timer"           -> List.of("type", "totalSeconds");
            case "notification"    -> List.of("type", "message");
            case "quiz"            -> List.of("type", "question", "options");
            case "exercise_card"   -> List.of("type", "exerciseName");
            case "exercise_phase"  -> List.of("type", "exerciseName", "phases");
            case "meal_chart"      -> List.of("type", "totalCalories");
            case "meal_plan"       -> List.of("type", "items");
            case "sleep_chart"     -> List.of("type", "totalHours");
            default                -> List.of("type");
        };
    }

    private static ObjectNode buildProperties(String type) {
        ObjectNode props = MAPPER.createObjectNode();

        // 所有 Widget 的公共属性
        props.set("type", stringSchema("组件类型标识", type));
        props.set("title", stringSchema("组件标题（可选）", null));

        // 按类型追加特有属性
        switch (type) {
            case "text_block" -> {
                props.set("content", stringSchema("文本内容（支持 Markdown）", null));
                props.set("textSize", stringSchema("字体大小: small/medium/large", "medium"));
                props.set("bold", boolSchema("是否加粗", false));
            }
            case "tip" -> {
                props.set("content", stringSchema("提示内容", null));
                props.set("icon", stringSchema("图标名称", "info"));
                props.set("category", stringSchema("分类: info/warning/success/danger", "info"));
            }
            case "stat_card" -> {
                props.set("metricName", stringSchema("指标名称", null));
                props.set("value", stringSchema("指标数值", null));
                props.set("unit", stringSchema("单位", ""));
                props.set("trend", stringSchema("变化趋势数值", ""));
                props.set("trendDirection", stringSchema("趋势方向: up/down/stable", "stable"));
                props.set("icon", stringSchema("图标名称", ""));
            }
            case "comparison" -> {
                props.set("beforeLabel", stringSchema("对比前标签", "之前"));
                props.set("beforeValue", stringSchema("对比前数值", null));
                props.set("afterLabel", stringSchema("对比后标签", "之后"));
                props.set("afterValue", stringSchema("对比后数值", null));
                props.set("changePercentage", stringSchema("变化百分比", ""));
                props.set("changeDirection", stringSchema("变化方向: increase/decrease/stable", "stable"));
            }
            case "progress_ring" -> {
                props.set("percentage", numSchema("完成百分比（0-100）", null));
                props.set("label", stringSchema("进度标签", ""));
                props.set("color", stringSchema("进度环颜色", "#58a6ff"));
                props.set("subText", stringSchema("副文本", ""));
            }
            case "timer" -> {
                props.set("totalSeconds", intSchema("总秒数", null));
                props.set("timerType", stringSchema("计时类型: countdown/stopwatch", "countdown"));
                props.set("startAction", stringSchema("开始操作标识", "timer:start"));
                props.set("pauseAction", stringSchema("暂停操作标识", "timer:pause"));
                props.set("resetAction", stringSchema("重置操作标识", "timer:reset"));
            }
            case "notification" -> {
                props.set("message", stringSchema("通知消息", null));
                props.set("severity", stringSchema("严重程度: info/warning/error/success", "info"));
                props.set("actionUrl", stringSchema("操作跳转URL", ""));
                props.set("actionLabel", stringSchema("操作按钮文字", ""));
                props.set("dismissible", boolSchema("是否可关闭", true));
            }
            case "quiz" -> {
                props.set("question", stringSchema("问题文本", null));
                props.set("options", arraySchema("选项列表", stringSchema("选项", null)));
                props.set("correctAnswer", stringSchema("正确答案（可空，为空时服务端校验）", ""));
                props.set("explanation", stringSchema("答案解析（可空）", ""));
                props.set("showResult", boolSchema("是否显示结果", false));
            }
            case "exercise_card" -> {
                props.set("exerciseName", stringSchema("运动名称", null));
                props.set("duration", intSchema("时长（分钟）", null));
                props.set("intensity", stringSchema("强度: 低/中/高", "中"));
                props.set("videoUrl", stringSchema("教学视频URL", ""));
                props.set("instruction", stringSchema("动作指导说明", ""));
                props.set("completed", boolSchema("是否已完成", false));
                props.set("checkinAction", stringSchema("打卡操作标识", "exercise:checkin"));
                props.set("scenarioTag", stringSchema("场景标签: 家庭/办公室/户外", ""));
                ObjectNode phaseItem = MAPPER.createObjectNode();
                phaseItem.set("name", stringSchema("阶段名称", null));
                phaseItem.set("type", stringSchema("阶段类型: warmup/core/cooldown", null));
                phaseItem.set("durationMinutes", intSchema("时长（分钟）", null));
                phaseItem.set("instruction", stringSchema("指导说明", ""));
                phaseItem.set("heartRateZone", stringSchema("心率区间", ""));
                phaseItem.set("completed", boolSchema("是否已完成", false));
                props.set("phases", arraySchema("运动阶段列表", phaseItem));
                props.set("completedPhases", intSchema("已完成阶段数", 0));
            }
            case "exercise_phase" -> {
                props.set("exerciseName", stringSchema("运动名称", null));
                props.set("totalDuration", intSchema("总时长（分钟）", null));
                props.set("intensity", stringSchema("强度", "中"));
                props.set("scenarioTag", stringSchema("场景标签", ""));
                ObjectNode phaseItem2 = MAPPER.createObjectNode();
                phaseItem2.set("name", stringSchema("阶段名称", null));
                phaseItem2.set("type", stringSchema("阶段类型", null));
                phaseItem2.set("durationMinutes", intSchema("时长", null));
                phaseItem2.set("instruction", stringSchema("指导说明", ""));
                phaseItem2.set("heartRateZone", stringSchema("心率区间", ""));
                phaseItem2.set("completed", boolSchema("是否已完成", false));
                props.set("phases", arraySchema("阶段列表", phaseItem2));
                props.set("completedPhases", intSchema("已完成阶段数", 0));
                props.set("videoUrl", stringSchema("视频URL", ""));
            }
            case "meal_chart" -> {
                props.set("totalCalories", intSchema("总热量（千卡）", null));
                props.set("protein", intSchema("蛋白质（克）", 0));
                props.set("carbs", intSchema("碳水（克）", 0));
                props.set("fat", intSchema("脂肪（克）", 0));
                props.set("remainingCalories", intSchema("剩余可摄入热量", 0));
                props.set("mealSuggestion", stringSchema("饮食建议", ""));
            }
            case "meal_plan" -> {
                props.set("mealType", stringSchema("餐食类型: breakfast/lunch/dinner/snack", "breakfast"));
                props.set("totalCalories", intSchema("总热量", 0));
                ObjectNode mealItem = MAPPER.createObjectNode();
                mealItem.set("name", stringSchema("食物名称", null));
                mealItem.set("calories", intSchema("热量（千卡）", 0));
                mealItem.set("protein", intSchema("蛋白质（克）", 0));
                mealItem.set("imageUrl", stringSchema("食物图片URL", ""));
                props.set("items", arraySchema("餐食条目列表", mealItem));
                ObjectNode nutritionProps = MAPPER.createObjectNode();
                nutritionProps.set("protein", intSchema("总蛋白质", 0));
                nutritionProps.set("carbs", intSchema("总碳水", 0));
                nutritionProps.set("fat", intSchema("总脂肪", 0));
                nutritionProps.set("fiber", intSchema("膳食纤维", 0));
                ObjectNode nutrition = MAPPER.createObjectNode();
                nutrition.put("type", "object");
                nutrition.set("properties", nutritionProps);
                props.set("nutrition", nutrition);
                props.set("cookingTip", stringSchema("烹饪建议", ""));
            }
            case "sleep_chart" -> {
                props.set("sleepScore", intSchema("睡眠评分（0-100）", 0));
                props.set("totalHours", numSchema("总睡眠时长（小时）", null));
                props.set("deepSleepHours", numSchema("深睡时长", 0d));
                props.set("lightSleepHours", numSchema("浅睡时长", 0d));
                props.set("remHours", numSchema("REM时长", 0d));
                ObjectNode sleepPhaseItem = MAPPER.createObjectNode();
                sleepPhaseItem.set("name", stringSchema("阶段名称", null));
                sleepPhaseItem.set("hours", numSchema("时长", 0d));
                sleepPhaseItem.set("color", stringSchema("显示颜色", "#58a6ff"));
                props.set("phases", arraySchema("睡眠阶段分布", sleepPhaseItem));
                props.set("suggestion", stringSchema("改善建议", ""));
            }
        }
        return props;
    }

    private static ArrayNode buildActions(String type) {
        return switch (type) {
            case "exercise_card" -> actions("checkin", "再生");
            case "exercise_phase" -> actions("checkin", "再生");
            case "timer" -> actions("start", "pause", "reset");
            case "quiz" -> actions("answer");
            case "notification" -> actions("dismiss", "click");
            default -> MAPPER.createArrayNode();
        };
    }

    private static ArrayNode buildDegradations(String type) {
        ArrayNode degradations = MAPPER.createArrayNode();
        // 所有 Widget 的最小降级策略：降级为 text_block
        degradations.add(MAPPER.createObjectNode()
                .put("targetClient", "any")
                .put("minVersion", "none")
                .put("fallbackType", "text_block")
                .put("fallbackRule", "提取 title + 关键字段拼接为纯文本"));
        return degradations;
    }

    // ======================== Schema 构建辅助 ========================

    private static ObjectNode stringSchema(String description, String defaultValue) {
        ObjectNode node = MAPPER.createObjectNode().put("type", "string").put("description", description);
        if (defaultValue != null) node.put("default", defaultValue);
        return node;
    }

    private static ObjectNode intSchema(String description, Integer defaultValue) {
        ObjectNode node = MAPPER.createObjectNode().put("type", "integer").put("description", description);
        if (defaultValue != null) node.put("default", defaultValue);
        return node;
    }

    private static ObjectNode numSchema(String description, Double defaultValue) {
        ObjectNode node = MAPPER.createObjectNode().put("type", "number").put("description", description);
        if (defaultValue != null) node.put("default", defaultValue);
        return node;
    }

    private static ObjectNode boolSchema(String description, Boolean defaultValue) {
        ObjectNode node = MAPPER.createObjectNode().put("type", "boolean").put("description", description);
        if (defaultValue != null) node.put("default", defaultValue);
        return node;
    }

    private static ObjectNode arraySchema(String description, ObjectNode itemSchema) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "array");
        node.put("description", description);
        node.set("items", itemSchema);
        return node;
    }

    private static ArrayNode actions(String... actionNames) {
        ArrayNode arr = MAPPER.createArrayNode();
        for (String name : actionNames) arr.add(name);
        return arr;
    }
}