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
            + "input_tokens, output_tokens, input_cost, output_cost, total_cost, latency_ms, success, "
            + "error_msg, create_time) VALUES (#{userId}, #{intent}, #{modelName}, #{modelTier}, "
            + "#{inputTokens}, #{outputTokens}, #{inputCost}, #{outputCost}, #{totalCost}, #{latencyMs}, "
            + "#{success}, #{errorMsg}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LlmCostLog log);

    /** 获取当日全局总成本 */
    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM llm_cost_log WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY")
    BigDecimal getGlobalDailyCost();

    /** 获取当日某用户总成本 */
    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM llm_cost_log WHERE user_id = #{userId} AND create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY")
    BigDecimal getUserDailyCost(@Param("userId") Long userId);

    /** 按意图统计当日某用户成本 */
    @Select("SELECT intent, COALESCE(SUM(total_cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE user_id = #{userId} AND create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY "
            + "GROUP BY intent ORDER BY cost DESC")
    List<Map<String, Object>> getUserDailyCostByIntent(@Param("userId") Long userId);

    /** 按模型统计当日某用户成本 */
    @Select("SELECT model_name, model_tier, COALESCE(SUM(total_cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE user_id = #{userId} AND create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY "
            + "GROUP BY model_name, model_tier ORDER BY cost DESC")
    List<Map<String, Object>> getUserDailyCostByModel(@Param("userId") Long userId);

    /** 按 Tier 统计当日全局成本 */
    @Select("SELECT model_tier, COALESCE(SUM(total_cost), 0) AS cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY GROUP BY model_tier")
    List<Map<String, Object>> getGlobalDailyCostByTier();

    /** 获取当日超预算用户（>1元） */
    @Select("SELECT user_id, COALESCE(SUM(total_cost), 0) AS total_cost, COUNT(*) AS call_count "
            + "FROM llm_cost_log WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY "
            + "GROUP BY user_id HAVING SUM(total_cost) > #{threshold}")
    List<Map<String, Object>> getOverBudgetUsers(@Param("threshold") BigDecimal threshold);

    /** 获取当日活跃用户数 */
    @Select("SELECT COUNT(DISTINCT user_id) FROM llm_cost_log WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY")
    long countActiveUsersToday();

    /** 清理过期日志 */
    @Delete("DELETE FROM llm_cost_log WHERE create_time < #{before}")
    int deleteOldLogs(@Param("before") LocalDateTime before);
}