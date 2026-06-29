package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.FoodItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodItemMapper extends BaseMapper<FoodItem> {
}