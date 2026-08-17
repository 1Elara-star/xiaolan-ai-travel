package com.lanyu.xiaolanaitravel.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlyAiHotelResponse {

    private FlyAiHotelData data;

    private String message;

    private Integer status;

    private String systemMessage;
}