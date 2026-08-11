package com.lanyu.xiaolanaitravel.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 旅行计划中的单个行程节点。
 *
 * <p>例如：第 1 天 10:00 游览大三巴，或第 2 天 19:30 观看演唱会。</p>
 *
 * <p>当前字段严格对应已有的 travel_plan_item 表。
 * 风险提醒和 Plan B 是后续 AI 行程设计的必要字段，
 * 待数据库存储方案确认后再补充，不在这里提前添加无对应列的属性。</p>
 */
@Data
@TableName("travel_plan_item")
public class TravelPlanItem {

    /** 行程节点ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属旅行计划ID */
    private Long planId;

    /** 行程中的第几天 */
    private Integer dayNumber;

    /** 当天节点的显示顺序 */
    private Integer itemOrder;

    /** 节点类型，例如 ATTRACTION、FOOD、HOTEL、EVENT、REST、OTHER */
    private String itemType;

    /** 关联的景点ID；非景点节点可以为空 */
    private Long attractionId;

    /** 地点名称 */
    private String placeName;

    /** 地点地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 节点开始时间 */
    private LocalTime startTime;

    /** 节点结束时间 */
    private LocalTime endTime;

    /** 从上一个节点到此处的交通方式 */
    private String transportMode;

    /** 与上一个节点的距离，单位由后续地图 API 方案统一确定 */
    private Integer distanceFromPrev;

    /** 从上一个节点出发所需时间，单位由后续地图 API 方案统一确定 */
    private Integer travelTimeFromPrev;

    /** 节点说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
