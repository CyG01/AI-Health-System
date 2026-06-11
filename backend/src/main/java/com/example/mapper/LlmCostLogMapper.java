package com.example.mapper;

import com.example.entity.LlmCostLog;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM 成本日志 Mapper（Phase 4：成本精细化）。
 *
 * 按用户×意图×模型维度记录 Token 消耗和费用。
 */
@Mapper
public interface LlmCostLogMapper {

    @Insert("INSERT INTO llm_cost_log (user_id, intent, model_name, model_tier, "
            + "prompt_tokens, completion_tokens, total_tokens, cost, latency_ms, success, "
            + "created_at) VALUES (#{userId}, #{intent}, #{modelName}, #{modelTier}, "
            + "#{promptTokens}, #{completionTokens}, #{totalTokens}, #{cost}, #{latencyMs}, "
            + "#{success}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LlmCostLog log);

    /** 获取当日全局总成本 */
    @Select("SELECT COALESCE(SUM(cost), 0) FROM llm_cost_log WHERE DATE(created_at) = CURDATE()")
    BigDecimal getGlobalDailyCost();

    /** 获取当日某用户总成本 */
    @Select("SELECT COALESCE(SUM(cost), 0) FROM llm_cost_log WHERE user_id = #{userId} AND DATE(created_at) = CURDATE()")
    BigDecimal getUserDailyCost(@Param("userId") Long userId);

    /** 按意图统计当日某用户成本 */
    @Select("SELECT intent, COALESCE(SUM(cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE user_id = #{userId} AND DATE(created_at) = CURDATE() "
            + "GROUP BY intent ORDER BY cost DESC")
    List<Map<String, Object>> getUserDailyCostByIntent(@Param("userId") Long userId);

    /** 按模型统计当日某用户成本 */
    @Select("SELECT model_name, model_tier, COALESCE(SUM(cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE user_id = #{userId} AND DATE(created_at) = CURDATE() "
            + "GROUP BY model_name, model_tier ORDER BY cost DESC")
    List<Map<String, Object>> getUserDailyCostByModel(@Param("userId") Long userId);

    /** 按 Tier 统计当日全局成本 */
    @Select("SELECT model_tier, COALESCE(SUM(cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE DATE(created_at) = CURDATE() GROUP BY model_tier")
    List<Map<String, Object>> getGlobalDailyCostByTier();

    /** 获取当日超预算用户（>1元） */
    @Select("SELECT user_id, COALESCE(SUM(cost), 0) AS total_cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE DATE(created_at) = CURDATE() "
            + "GROUP BY user_id HAVING SUM(cost) > #{threshold}")
    List<Map<String, Object>> getOverBudgetUsers(@Param("threshold") BigDecimal threshold);

    /** 获取当日活跃用户数 */
    @Select("SELECT COUNT(DISTINCT user_id) FROM llm_cost_log WHERE DATE(created_at) = CURDATE()")
    long countActiveUsersToday();

    /** 清理过期日志 */
    @Delete("DELETE FROM llm_cost_log WHERE created_at < #{before}")
    int deleteOldLogs(@Param("before") LocalDateTime before);
}