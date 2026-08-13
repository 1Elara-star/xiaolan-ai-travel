package com.lanyu.xiaolanaitravel.travel.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TravelPlanItemRequest(
        @NotNull(message = "行程天数不能为空")
        @Min(value = 1, message = "行程天数至少为1")
        Integer dayNumber,

        @NotNull(message = "节点顺序不能为空")
        @Min(value = 1, message = "节点顺序至少为1")
        Integer itemOrder,

        @NotBlank(message = "节点类型不能为空")
        @Pattern(regexp = "ATTRACTION|FOOD|SHOPPING|HOTEL|EVENT|REST|OTHER",
                message = "节点类型不支持")
        String itemType,

        Long attractionId,

        @NotBlank(message = "地点名称不能为空")
        @Size(max = 150, message = "地点名称不能超过150字")
        String placeName,

        @Size(max = 255, message = "地址不能超过255字")
        String address,

        @DecimalMin(value = "-180", message = "经度不能小于-180")
        @DecimalMax(value = "180", message = "经度不能大于180")
        BigDecimal longitude,

        @DecimalMin(value = "-90", message = "纬度不能小于-90")
        @DecimalMax(value = "90", message = "纬度不能大于90")
        BigDecimal latitude,

        LocalTime startTime,
        LocalTime endTime,

        @Size(max = 30, message = "交通方式不能超过30字")
        String transportMode,

        @Min(value = 0, message = "距离不能小于0")
        Integer distanceFromPrev,

        @Min(value = 0, message = "交通时间不能小于0")
        Integer travelTimeFromPrev,

        @Size(max = 500, message = "节点说明不能超过500字")
        String description) {
}
