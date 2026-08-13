package com.lanyu.xiaolanaitravel.travel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建/修改旅行计划时的请求参数
 *
 * 这里只接收用户真正需要填写的旅行需求。
 * userId、旅行天数、旅行状态、AI行程内容等
 * 由后端自动处理，不允许前端随意传入。
 */
@Data
public class TravelPlanRequest {

    /**
     * 旅行标题
     * 例如：澳门演唱会之旅
     */
    @NotBlank(message = "旅行标题不能为空")
    @Size(max = 100, message = "旅行标题不能超过100字")
    private String title;

    /**
     * 出发城市
     */
    @NotBlank(message = "出发城市不能为空")
    @Size(max = 100, message = "出发城市不能超过100字")
    private String departureCity;

    /**
     * 目的地
     */
    @NotBlank(message = "目的地不能为空")
    @Size(max = 100, message = "目的地不能超过100字")
    private String destination;

    /**
     * 出发日期
     */
    @NotNull(message = "出发日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 出行人数
     */
    @NotNull(message = "出行人数不能为空")
    @Min(value = 1, message = "出行人数至少为1人")
    private Integer peopleCount;

    /**
     * 同行类型
     * 例如：一个人、朋友、情侣、家人
     */
    @Size(max = 30, message = "同行类型不能超过30字")
    private String companionType;

    /**
     * 本次旅行总预算
     */
    @DecimalMin(value = "0.0", inclusive = false,
            message = "旅行预算必须大于0")
    private BigDecimal budget;

    /**
     * 旅行类型
     * 例如：演唱会旅行、休闲旅行、美食旅行
     */
    @Size(max = 50, message = "旅行类型不能超过50字")
    private String tripType;

    /**
     * 本次旅行特别偏好
     *
     * 例如：
     * 想多拍照、
     * 想吃当地特色、
     * 不希望行程太赶。
     */
    @Size(max = 500, message = "本次旅行偏好不能超过500字")
    private String tripPreferences;

    /**
     * 本次旅行特殊要求
     *
     * 例如：
     * 第一次独自旅行、
     * 第一次坐飞机、
     * 演唱会晚上结束较晚、
     * 希望提前规划返程和Plan B。
     */
    @Size(max = 1000, message = "特殊要求不能超过1000字")
    private String specialRequirements;
}
