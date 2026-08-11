package com.lanyu.xiaolanaitravel.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旅行计划数据访问接口
 *
 * 通过 MyBatis-Plus 操作 travel_plan 表。
 */
@Mapper
public interface TravelPlanMapper extends BaseMapper<TravelPlan> {

}