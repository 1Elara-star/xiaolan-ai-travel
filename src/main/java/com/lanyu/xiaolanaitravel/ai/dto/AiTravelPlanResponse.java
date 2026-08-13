package com.lanyu.xiaolanaitravel.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiTravelPlanResponse {

    private String destination;

    private Integer travelDays;

    private String summary;

    private List<AiTravelDay> days;
}