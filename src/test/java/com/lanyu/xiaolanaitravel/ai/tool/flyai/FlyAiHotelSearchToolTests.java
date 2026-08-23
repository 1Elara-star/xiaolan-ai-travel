package com.lanyu.xiaolanaitravel.ai.tool.flyai;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelData;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelItem;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import com.lanyu.xiaolanaitravel.ai.service.FlyAiService;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolRequest;
import com.lanyu.xiaolanaitravel.ai.tool.flyai.dto.FlyAiHotelSearchToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlyAiHotelSearchToolTests {

    @Mock
    private FlyAiService flyAiService;

    private FlyAiHotelSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new FlyAiHotelSearchTool(flyAiService);
    }

    @Test
    void shouldReturnStableHotelResultsAndApplyLimit() {
        FlyAiHotelResponse response = responseWith(
                hotel("酒店A", "399"),
                hotel("酒店B", "499")
        );
        when(flyAiService.searchHotels("成都", "春熙路", 600)).thenReturn(response);

        FlyAiHotelSearchToolResult result = tool.execute(
                new FlyAiHotelSearchToolRequest(" 成都 ", " 春熙路 ", 600, 1)
        );

        assertEquals(1, result.count());
        assertEquals("酒店A", result.hotels().get(0).name());
        assertEquals("399", result.hotels().get(0).price());
        assertEquals("https://example.com/hotel.jpg", result.hotels().get(0).imageUrl());
        verify(flyAiService).searchHotels("成都", "春熙路", 600);
    }

    @Test
    void shouldReturnEmptyResultWhenFlyAiHasNoData() {
        when(flyAiService.searchHotels("厦门", null, null)).thenReturn(new FlyAiHotelResponse());

        FlyAiHotelSearchToolResult result = tool.execute(
                new FlyAiHotelSearchToolRequest("厦门", " ", null, null)
        );

        assertEquals(0, result.count());
        assertEquals(List.of(), result.hotels());
    }

    @Test
    void shouldRejectInvalidPriceWithoutCallingFlyAi() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(
                new FlyAiHotelSearchToolRequest("成都", null, -1, 5)
        ));

        verify(flyAiService, never()).searchHotels("成都", null, -1);
    }

    private FlyAiHotelResponse responseWith(FlyAiHotelItem... hotels) {
        FlyAiHotelData data = new FlyAiHotelData();
        data.setItemList(List.of(hotels));

        FlyAiHotelResponse response = new FlyAiHotelResponse();
        response.setData(data);
        return response;
    }

    private FlyAiHotelItem hotel(String name, String price) {
        FlyAiHotelItem hotel = new FlyAiHotelItem();
        hotel.setName(name);
        hotel.setPrice(price);
        hotel.setAddress("测试地址");
        hotel.setLongitude("104.066000");
        hotel.setLatitude("30.572000");
        hotel.setMainPic("https://example.com/hotel.jpg");
        hotel.setDetailUrl("https://example.com/hotel-detail");
        hotel.setStar("舒适型");
        hotel.setBrandName("测试品牌");
        return hotel;
    }
}
