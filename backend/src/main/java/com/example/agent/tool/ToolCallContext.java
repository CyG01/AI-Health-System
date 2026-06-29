package com.example.agent.tool;

import com.example.agent.model.ToolCallRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tool 调用上下文 — 基于 ThreadLocal 的线程安全记录器。
 * <p>
 * 使用方式：
 * <pre>
 *   ToolCallContext.start();
 *   try {
 *       agent.generatePlan(userMessage);  // LangChain4j 内部触发 Tool 调用
 *       List&lt;ToolCallRecord&gt; records = ToolCallContext.getRecords();
 *   } finally {
 *       ToolCallContext.clear();
 *   }
 * </pre>
 */
public final class ToolCallContext {

    private static final ThreadLocal<List<ToolCallRecord>> RECORDS = ThreadLocal.withInitial(ArrayList::new);

    private ToolCallContext() {}

    /** 开始新一轮记录（清空之前的数据） */
    public static void start() {
        RECORDS.get().clear();
    }

    /** 添加一条 Tool 调用记录 */
    public static void addRecord(ToolCallRecord record) {
        RECORDS.get().add(record);
    }

    /** 获取当前线程所有 Tool 调用记录（不可变副本） */
    public static List<ToolCallRecord> getRecords() {
        return Collections.unmodifiableList(new ArrayList<>(RECORDS.get()));
    }

    /** 是否有 Tool 被调用 */
    public static boolean hasRecords() {
        return !RECORDS.get().isEmpty();
    }

    /** 记录数量 */
    public static int count() {
        return RECORDS.get().size();
    }

    /** 清空并移除 ThreadLocal（防内存泄漏） */
    public static void clear() {
        RECORDS.get().clear();
        RECORDS.remove();
    }
}