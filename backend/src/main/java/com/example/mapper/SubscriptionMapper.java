package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {

    @Select("SELECT * FROM subscription WHERE user_id = #{userId} AND status = 'active' ORDER BY id DESC LIMIT 1")
    Subscription findActiveByUserId(Long userId);
}