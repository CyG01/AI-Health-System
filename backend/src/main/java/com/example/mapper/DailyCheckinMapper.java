package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.DailyCheckin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyCheckinMapper extends BaseMapper<DailyCheckin> {
}
