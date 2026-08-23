package com.lanyu.xiaolanaitravel.amap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.amap.dto.AmapRouteApiResponse;
import com.lanyu.xiaolanaitravel.amap.dto.AmapTransitRouteResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class AmapServiceTransitParsingTests {

    @Test
    void shouldMapSnakeCaseScheduleFieldsWithoutCallingAmap() throws Exception {
        String json = """
                {
                  "status": "1",
                  "infocode": "10000",
                  "route": {
                    "transits": [{
                      "distance": "2584",
                      "nightflag": "0",
                      "cost": {"duration": "2100"},
                      "segments": [{
                        "bus": {
                          "buslines": [{
                            "name": "厦门地铁1号线",
                            "type": "地铁线路",
                            "distance": "22000",
                            "duration": "1800",
                            "departure_stop": {"name": "镇海路"},
                            "arrival_stop": {"name": "高崎"},
                            "start_time": "0600",
                            "end_time": "2330",
                            "station_start_time": "0615",
                            "station_end_time": "2305"
                          }]
                        }
                      }]
                    }]
                  }
                }
                """;

        AmapRouteApiResponse response = new ObjectMapper().readValue(
                json, AmapRouteApiResponse.class);
        AmapService service = new AmapService(mock(RestClient.class), "unused-test-key");

        AmapTransitRouteResult result = service.parseTransitRouteDetails(response);

        assertEquals(2584, result.distanceMeters());
        assertEquals(2100, result.durationSeconds());
        assertFalse(result.nightRoute());
        assertEquals("06:00", result.lines().get(0).lineStartTime());
        assertEquals("23:30", result.lines().get(0).lineEndTime());
        assertEquals("06:15", result.lines().get(0).boardingStationStartTime());
        assertEquals("23:05", result.lines().get(0).boardingStationEndTime());
    }
}
