package com.lanyu.xiaolanaitravel.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlyAiHotelData {

    private List<FlyAiHotelItem> itemList;
}