package com.lanyu.xiaolanaitravel.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlyAiHotelItem {

    private String name;
    private String price;
    private String address;
    private String latitude;
    private String longitude;
    private String mainPic;
    private String detailUrl;
    private String star;
    private String brandName;
}