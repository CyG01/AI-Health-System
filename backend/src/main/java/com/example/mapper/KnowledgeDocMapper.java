package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDoc> {

    /**
     * 基于向量余弦相似度检索知识文档（分级过滤）
     */
    @Select("<script>" +
            "SELECT id, title, content, category, source_name, authority_level, version, " +
            "VEC_COSINE_DISTANCE(embedding, #{embedding}) AS distance " +
            "FROM knowledge_doc " +
            "WHERE is_active = 1 AND embedding IS NOT NULL " +
            "<if test='authorityLevels != null and authorityLevels.size() > 0'>" +
            "  AND authority_level IN " +
            "  <foreach collection='authorityLevels' item='level' open='(' separator=',' close=')'>" +
            "    #{level}" +
            "  </foreach>" +
            "</if>" +
            "ORDER BY distance ASC LIMIT #{topK}" +
            "</script>")
    List<KnowledgeDoc> findSimilar(@Param("embedding") String embedding,
                                   @Param("authorityLevels") List<String> authorityLevels,
                                   @Param("topK") int topK);

    /**
     * 按分类和权威等级检索
     */
    @Select("<script>" +
            "SELECT * FROM knowledge_doc WHERE is_active = 1 " +
            "<if test='category != null'> AND category = #{category} </if>" +
            "<if test='authorityLevel != null'> AND authority_level = #{authorityLevel} </if>" +
            "ORDER BY authority_level ASC, updated_at DESC LIMIT #{limit}" +
            "</script>")
    List<KnowledgeDoc> findByCategoryAndLevel(@Param("category") String category,
                                               @Param("authorityLevel") String authorityLevel,
                                               @Param("limit") int limit);

    /**
     * 统计各等级文档数量
     */
    @Select("SELECT authority_level, COUNT(*) AS cnt FROM knowledge_doc WHERE is_active = 1 GROUP BY authority_level")
    List<java.util.Map<String, Object>> countByLevel();

    /**
     * 分页查询（用于批量构建向量索引）
     */
    @Select("SELECT * FROM knowledge_doc WHERE is_active = 1 ORDER BY id LIMIT #{offset}, #{limit}")
    List<KnowledgeDoc> selectPage(@Param("offset") long offset, @Param("limit") int limit);

    /**
     * MySQL LIKE 模糊搜索（Qdrant 不可用时的降级方案）
     */
    @Select("<script>" +
            "SELECT * FROM knowledge_doc WHERE is_active = 1 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "<if test='authorityLevels != null and authorityLevels.size() > 0'>" +
            "  AND authority_level IN " +
            "  <foreach collection='authorityLevels' item='level' open='(' separator=',' close=')'>" +
            "    #{level}" +
            "  </foreach>" +
            "</if>" +
            "ORDER BY authority_level ASC LIMIT #{limit}" +
            "</script>")
    List<KnowledgeDoc> searchByKeyword(@Param("keyword") String keyword,
                                        @Param("authorityLevels") List<String> authorityLevels,
                                        @Param("limit") int limit);

    /**
     * 统计活跃文档数
     */
    @Select("SELECT COUNT(*) FROM knowledge_doc WHERE is_active = 1")
    long count();

    /**
     * 查询所有活跃文档（用于批量索引）
     */
    @Select("SELECT * FROM knowledge_doc WHERE is_active = 1")
    List<KnowledgeDoc> findAllActive();
}