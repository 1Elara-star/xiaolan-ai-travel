package com.lanyu.xiaolanaitravel.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旅行行程节点数据访问接口。
 *
 * <p>通过 MyBatis-Plus 操作 travel_plan_item 表，
 * 当前没有复杂 SQL，因此直接使用 BaseMapper。</p>
 */
@Mapper
public interface TravelPlanItemMapper extends BaseMapper<TravelPlanItem> {
}
