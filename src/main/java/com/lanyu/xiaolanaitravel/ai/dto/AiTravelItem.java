package com.lanyu.xiaolanaitravel.ai.dto;

import lombok.Data;

@Data
public class AiTravelItem {

    /** 使用用户收藏景点时返回对应景点 ID；其他节点为空。 */
    private Long attractionId;

    private String placeName;

    private String startTime;

    private String endTime;

    /** 结束时间相对开始日的偏移量：0=当天，1=次日 */
    private Integer endDayOffset;

    private String itemType;

    private String description;
}
