package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.UserMemory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMemoryMapper extends BaseMapper<UserMemory> {

    /**
     * 基于向量余弦相似度检索最相关记忆（Top-K）。
     * 当前实现使用 MySQL VEC_COSINE_DISTANCE 函数（需原生 VECTOR 类型）。
     * 若使用 TEXT 存储向量，可改用应用层余弦相似度计算。
     */
    @Select("SELECT id, user_id, memory_type, content, importance, source, access_count, last_accessed_at, created_at, "
            + "VEC_COSINE_DISTANCE(embedding, #{embedding}) AS distance "
            + "FROM user_memory "
            + "WHERE user_id = #{userId} AND embedding IS NOT NULL "
            + "ORDER BY distance ASC LIMIT #{topK}")
    List<UserMemory> findSimilar(@Param("userId") Long userId,
                                 @Param("embedding") String embedding,
                                 @Param("topK") int topK);

    /**
     * 获取用户所有有向量的记忆（用于应用层余弦相似度计算降级方案）
     */
    @Select("SELECT * FROM user_memory WHERE user_id = #{userId} AND embedding IS NOT NULL")
    List<UserMemory> findAllWithEmbedding(@Param("userId") Long userId);

    /**
     * 获取用户高重要性记忆（≥7，永不删除）
     */
    @Select("SELECT * FROM user_memory WHERE user_id = #{userId} AND importance >= 7 ORDER BY created_at DESC")
    List<UserMemory> findHighImportance(@Param("userId") Long userId);

    /**
     * 清理低重要性旧记忆（90天未访问且importance < 3）
     */
    @Delete("DELETE FROM user_memory WHERE last_accessed_at < #{threshold} AND importance < 3")
    int deleteLowImportanceStale(@Param("threshold") String threshold);

    /**
     * 更新记忆访问统计
     */
    @Update("UPDATE user_memory SET access_count = access_count + 1, last_accessed_at = NOW() WHERE id = #{id}")
    int incrementAccessCount(@Param("id") Long id);

    /**
     * 获取用户指定类型的记忆数量
     */
    @Select("SELECT COUNT(*) FROM user_memory WHERE user_id = #{userId} AND memory_type = #{memoryType}")
    int countByType(@Param("userId") Long userId, @Param("memoryType") String memoryType);
}