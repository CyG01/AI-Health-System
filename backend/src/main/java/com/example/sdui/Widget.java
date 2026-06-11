package com.example.sdui;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * SDUI 组件抽象基类。
 * 前端根据 type 字段动态渲染对应组件，未知类型降级为纯文本展示。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExerciseCardWidget.class, name = "exercise_card"),
        @JsonSubTypes.Type(value = ExercisePhaseWidget.class, name = "exercise_phase"),
        @JsonSubTypes.Type(value = MealChartWidget.class, name = "meal_chart"),
        @JsonSubTypes.Type(value = ProgressRingWidget.class, name = "progress_ring"),
        @JsonSubTypes.Type(value = TimerWidget.class, name = "timer"),
        @JsonSubTypes.Type(value = NotificationWidget.class, name = "notification"),
        @JsonSubTypes.Type(value = QuizWidget.class, name = "quiz"),
        @JsonSubTypes.Type(value = StatCardWidget.class, name = "stat_card"),
        @JsonSubTypes.Type(value = ComparisonWidget.class, name = "comparison"),
        @JsonSubTypes.Type(value = TipWidget.class, name = "tip"),
        @JsonSubTypes.Type(value = TextBlockWidget.class, name = "text_block"),
})
public abstract class Widget {

    protected String type;
    protected String title;
    protected Map<String, Object> props = new HashMap<>();

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props;
    }

    public void putProp(String key, Object value) {
        if (this.props == null) {
            this.props = new HashMap<>();
        }
        this.props.put(key, value);
    }
}