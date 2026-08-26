package com.lanyu.xiaolanaitravel.ai.tool.flyai;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelData;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelItem;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolItem;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * AI 酒店查询 Tool。
 *
 * <p>该组件只负责把 AI 层的结构化查询转换成现有飞猪酒店查询，
 * 再把第三方 DTO 转换成稳定的 Tool 输出。它不负责用户权限、旅行计划、
 * 用户画像排序、酒店选择或数据库保存。</p>
 */
@Component
public class FlyAiHotelSearchTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private final FlyAiService flyAiService;

    public FlyAiHotelSearchTool(FlyAiService flyAiService) {
        this.flyAiService = flyAiService;
    }

    /**
     * 提供给 Spring AI 的真实酒店候选查询入口。
     *
     * <p>当前仅声明 Tool 元数据，是否执行仍由受控 Planner Workflow 决定。</p>
     */
    @Tool(
            name = "flyAiHotelSearch",
            description = "查询旅行目的地的真实酒店候选，返回平台提供的名称、价格、地址、坐标、图片和详情链接。可按商圈、交通枢纽、地铁站、景区、地标、行政区或自定义地点限定住宿位置。"
    )
    public FlyAiHotelSearchToolResult searchHotels(
            @ToolParam(description = "旅行目的地城市，例如成都、厦门") String destination,
            @ToolParam(description = "可选住宿位置，可填写商圈、交通枢纽、地铁站、景区、地标、行政区或自定义地点", required = false) String locationKeyword,
            @ToolParam(description = "可选每晚最高价格，单位为人民币元", required = false) Integer maxPrice,
            @ToolParam(description = "可选返回数量，允许1到10，缺省时使用5", required = false) Integer limit) {
        return execute(new FlyAiHotelSearchToolRequest(
                destination,
                locationKeyword,
                maxPrice,
                limit
        ));
    }

    /**
     * 查询真实酒店候选，并限制返回给 AI 的候选数量。
     */
    public FlyAiHotelSearchToolResult execute(FlyAiHotelSearchToolRequest request) {
        validateRequest(request);

        String destination = request.destination().strip();
        String locationKeyword = normalizeOptional(request.locationKeyword());
        int limit = normalizeLimit(request.limit());

        FlyAiHotelResponse response = flyAiService.searchHotels(
                destination,
                locationKeyword,
                request.maxPrice()
        );

        List<FlyAiHotelSearchToolItem> hotels = extractHotels(response).stream()
                .filter(Objects::nonNull)
                .limit(limit)
                .map(this::convert)
                .toList();

        return new FlyAiHotelSearchToolResult(hotels, hotels.size());
    }

    private List<FlyAiHotelItem> extractHotels(FlyAiHotelResponse response) {
        if (response == null) {
            return List.of();
        }
        FlyAiHotelData data = response.getData();
        if (data == null || data.getItemList() == null) {
            return List.of();
        }
        return data.getItemList();
    }

    private FlyAiHotelSearchToolItem convert(FlyAiHotelItem hotel) {
        return new FlyAiHotelSearchToolItem(
                hotel.getName(),
                hotel.getPrice(),
                hotel.getAddress(),
                hotel.getLongitude(),
                hotel.getLatitude(),
                hotel.getMainPic(),
                hotel.getDetailUrl(),
                hotel.getStar(),
                hotel.getBrandName()
        );
    }

    private void validateRequest(FlyAiHotelSearchToolRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("酒店查询参数不能为空");
        }
        if (request.destination() == null || request.destination().isBlank()) {
            throw new IllegalArgumentException("酒店目的地不能为空");
        }
        if (request.maxPrice() != null && request.maxPrice() < 0) {
            throw new IllegalArgumentException("酒店最高价格不能小于0");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
