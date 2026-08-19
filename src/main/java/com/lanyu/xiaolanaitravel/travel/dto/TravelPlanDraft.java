package com.lanyu.xiaolanaitravel.travel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 尚未最终保存的候选旅行方案。
 *
 * DeepSeek 生成行程后，先转换成 Draft。
 * 后续高德补全、路线计算、Workflow 校验、
 * Repair 等操作都先作用在 Draft 上。
 *
 * 只有最终确认无严重问题后，
 * 才转换成 TravelPlanItem 并保存数据库。
 */
@Data
public class TravelPlanDraft {

    /**
     * 对应数据库中的 TravelPlan ID。
     *
     * 由后端提供，不由 AI 生成。
     */
    private Long planId;

    /**
     * 旅行目的地。
     *
     * 高德 POI 搜索时可以用来限制搜索城市。
     */
    private String destination;

    /**
     * 总旅行天数。
     */
    private Integer travelDays;

    /**
     * AI 对整份旅行方案的总体说明。
     */
    private String summary;

    /**
     * 全部候选行程节点。
     *
     * 节点通过 dayNumber + itemOrder
     * 表示属于哪一天以及当天顺序。
     */
    private List<TravelPlanDraftItem> items = new ArrayList<>();
}