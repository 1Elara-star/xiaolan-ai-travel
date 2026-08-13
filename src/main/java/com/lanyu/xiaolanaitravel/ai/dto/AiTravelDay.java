package com.lanyu.xiaolanaitravel.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiTravelDay {

    private Integer dayNumber;

    private String theme;

    private List<AiTravelItem> items;
}