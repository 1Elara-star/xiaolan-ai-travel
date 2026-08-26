package com.lanyu.xiaolanaitravel.ai.tool.amap;

import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitLineToolResult;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.amap.dto.AmapTransitRouteToolResult;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitRouteResult;
import com.lanyu.xiaolanaitravel.amap.service.AmapService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * AI 公交路线查询 Tool。
 *
 * <p>查询公交/地铁线路、距离、耗时、上下车站和高德返回的首末班时间。
 * 该 Tool 不判断用户是否一定能赶上末班车，也不修改行程或数据库。</p>
 */
@Component
public class AmapTransitRouteTool {

    private final AmapService amapService;

    public AmapTransitRouteTool(AmapService amapService) {
        this.amapService = amapService;
    }

    /**
     * 提供给 Spring AI 的公交/地铁路线查询入口。
     *
     * <p>当前仅声明 Tool 元数据，是否执行仍由受控 Planner Workflow 决定。</p>
     */
    @Tool(
            name = "amapTransitRoute",
            description = "根据已经确认的起终点经纬度和城市编码，查询中国境内真实公交或地铁路线、距离、耗时、上下车站及高德可提供的首末班时间。地点尚未获得坐标时不要使用。"
    )
    public AmapTransitRouteToolResult searchTransitRoute(
            @ToolParam(description = "起点经度") BigDecimal originLongitude,
            @ToolParam(description = "起点纬度") BigDecimal originLatitude,
            @ToolParam(description = "终点经度") BigDecimal destinationLongitude,
            @ToolParam(description = "终点纬度") BigDecimal destinationLatitude,
            @ToolParam(description = "起点城市编码，例如0592") String originCityCode,
            @ToolParam(description = "终点城市编码，例如0592") String destinationCityCode,
            @ToolParam(description = "可选出发日期，格式yyyy-MM-dd", required = false) String departureDate,
            @ToolParam(description = "可选出发时间，格式HH:mm", required = false) String departureTime,
            @ToolParam(description = "是否考虑夜班公交，缺省时按否处理", required = false) Boolean considerNightBus) {
        return execute(new AmapTransitRouteToolRequest(
                originLongitude,
                originLatitude,
                destinationLongitude,
                destinationLatitude,
                originCityCode,
                destinationCityCode,
                departureDate,
                departureTime,
                considerNightBus
        ));
    }

    public AmapTransitRouteToolResult execute(AmapTransitRouteToolRequest request) {
        validateRequest(request);

        LocalDate departureDate = parseDate(request.departureDate());
        LocalTime departureTime = parseTime(request.departureTime());
        AmapTransitRouteResult route = amapService.calculateTransitRoute(
                request.originLongitude(),
                request.originLatitude(),
                request.destinationLongitude(),
                request.destinationLatitude(),
                request.originCityCode().strip(),
                request.destinationCityCode().strip(),
                departureDate,
                departureTime,
                Boolean.TRUE.equals(request.considerNightBus())
        );

        int durationMinutes = Math.max(1, (route.durationSeconds() + 59) / 60);
        return new AmapTransitRouteToolResult(
                route.distanceMeters(),
                route.durationSeconds(),
                durationMinutes,
                route.nightRoute(),
                normalizeOptional(request.departureDate()),
                normalizeOptional(request.departureTime()),
                route.lines().stream()
                        .map(line -> new AmapTransitLineToolResult(
                                line.lineName(),
                                line.lineType(),
                                line.departureStopName(),
                                line.arrivalStopName(),
                                line.distanceMeters(),
                                line.durationSeconds(),
                                line.lineStartTime(),
                                line.lineEndTime(),
                                line.boardingStationStartTime(),
                                line.boardingStationEndTime()
                        ))
                        .toList()
        );
    }

    private void validateRequest(AmapTransitRouteToolRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("公交路线查询参数不能为空");
        }
        if (request.originLongitude() == null || request.originLatitude() == null
                || request.destinationLongitude() == null
                || request.destinationLatitude() == null) {
            throw new IllegalArgumentException("公交路线起终点必须包含经纬度");
        }
        if (request.originCityCode() == null || request.originCityCode().isBlank()
                || request.destinationCityCode() == null
                || request.destinationCityCode().isBlank()) {
            throw new IllegalArgumentException("公交路线起终点必须包含城市编码");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("出发日期必须使用 yyyy-MM-dd 格式", exception);
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.strip());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("出发时间必须使用 HH:mm 格式", exception);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
