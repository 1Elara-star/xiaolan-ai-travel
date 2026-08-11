package com.lanyu.xiaolanaitravel.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅行计划实体类
 *
 * 一条记录代表用户的一次具体旅行。
 */
@Data
@TableName("travel_plan")
public class TravelPlan {

    /**
     * 旅行计划ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建该旅行的用户ID
     */
    private Long userId;

    /**
     * 旅行标题
     * 例如：澳门演唱会之旅
     */
    private String title;

    /**
     * 出发城市
     */
    private String departureCity;

    /**
     * 目的地
     */
    private String destination;

    /**
     * 出发日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 旅行天数
     */
    private Integer travelDays;

    /**
     * 出行人数
     */
    private Integer peopleCount;

    /**
     * 同行类型
     * 例如：一个人、朋友、情侣、家人
     */
    private String companionType;

    /**
     * 总预算
     */
    private BigDecimal budget;

    /**
     * 旅行类型
     * 例如：演唱会旅行、休闲旅行、美食旅行
     */
    private String tripType;

    /**
     * 本次旅行的特别偏好
     *
     * 注意：
     * 这是“这一次旅行”的偏好，
     * 与 user_profile 中的长期偏好不同。
     */
    private String tripPreferences;

    /**
     * 特殊要求
     *
     * 例如：
     * 第一次独自出行、
     * 晚上演唱会结束较晚、
     * 希望提前规划返程方案。
     */
    private String specialRequirements;

    /**
     * AI生成的完整行程方案快照
     *
     * 后期真正可编辑的每日行程
     * 会存入 travel_plan_item。
     */
    private String planContent;

    /**
     * 旅行状态
     *
     * PLANNING 规划中
     * UPCOMING 即将出发
     * ONGOING 旅行中
     * FINISHED 已结束
     */
    private String tripStatus = "PLANNING";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}