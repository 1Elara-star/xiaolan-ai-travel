package com.lanyu.xiaolanaitravel.travel.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 候选旅行方案中的单个节点。
 *
 * 该对象尚未进入数据库，
 * 可以在内存中被 AI、高德、Workflow 和 Repair 不断补充或修改。
 */
@Data
public class TravelPlanDraftItem {

    /*
     * =========================
     * 候选节点身份信息
     * =========================
     */

    /**
     * 候选节点临时唯一标识。
     *
     * 例如：
     * D1-I1
     * D1-I2
     * D2-I1
     *
     * 因为候选节点尚未保存数据库，
     * 此时还没有 TravelPlanItem.id，
     * 所以需要一个临时标识供 Workflow 和 Repair 使用。
     */
    private String draftItemKey;

    /** 关联本地景点；收藏景点命中时由后端校验并填写。 */
    private Long attractionId;

    /** FAVORITE / LOCAL_ATTRACTION / AI_RECOMMENDED。 */
    private String sourceType;

    /**
     * 行程中的第几天。
     */
    private Integer dayNumber;

    /**
     * 当天第几个节点。
     *
     * 由 Java 后端根据 AI 返回顺序生成。
     */
    private Integer itemOrder;


    /*
     * =========================
     * AI 规划字段
     * =========================
     */

    /**
     * 节点类型。
     *
     * ATTRACTION / FOOD / HOTEL /
     * EVENT / REST / OTHER
     */
    private String itemType;

    /**
     * AI 原始规划的地点名称。
     */
    private String placeName;

    /**
     * 节点开始时间。
     */
    private LocalTime startTime;

    /**
     * 节点结束时间。
     */
    private LocalTime endTime;

    /**
     * 结束时间相对于开始日期的跨天偏移。
     *
     * 0 = 当天结束
     * 1 = 次日结束
     */
    private Integer endDayOffset;

    /**
     * AI 对该节点的安排说明。
     */
    private String description;

    /*
     * =========================
     * 本地景点展示信息
     * =========================
     */

    private String imageUrl;
    private String storyBackground;
    private String featureDescription;
    private String suitableTags;
    private Integer suggestDuration;
    private String openTime;
    private String ticketInfo;


    /*
     * =========================
     * 高德 POI 补全字段
     * =========================
     */

    /**
     * 高德 POI ID。
     */
    private String poiId;

    /**
     * 高德真正匹配到的 POI 名称。
     *
     * 不直接覆盖 placeName，
     * 方便后续判断：
     * AI 想去的地方和高德匹配结果是否一致。
     */
    private String matchedPoiName;

    /**
     * 真实地址。
     */
    private String address;

    /**
     * 经度。
     */
    private BigDecimal longitude;

    /**
     * 纬度。
     */
    private BigDecimal latitude;

    /**
     * 高德城市编码。
     *
     * 公交路线规划时会使用。
     */
    private String cityCode;


    /*
     * =========================
     * 路线补全字段
     * =========================
     */

    /**
     * 从上一个有效地点到当前节点的交通方式。
     *
     * 例如：
     * WALKING
     * DRIVING
     * BICYCLING
     * TRANSIT
     */
    private String transportMode;

    /**
     * 与上一个地点之间的距离，单位：米。
     */
    private Integer distanceFromPrev;

    /**
     * 与上一个地点之间的交通耗时，单位：分钟。
     */
    private Integer travelTimeFromPrev;

    private Integer straightLineDistanceFromPrev;
}
