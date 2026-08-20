package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.controller.TravelDraftMapController;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftEnrichmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelDraftMapControllerTests {

    @Test
    void delegatesMapEnrichmentForCurrentUser() {
        TravelDraftEnrichmentService enrichmentService =
                mock(TravelDraftEnrichmentService.class);
        TravelDraftSessionResponse expected =
                new TravelDraftSessionResponse(
                        "draft-1",
                        LocalDateTime.of(2026, 8, 20, 10, 30),
                        new TravelPlanDraft()
                );
        when(enrichmentService.enrichMap(7L, "draft-1"))
                .thenReturn(expected);

        var response =
                new TravelDraftMapController(
                        enrichmentService
                ).enrichMap(7L, "draft-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(enrichmentService)
                .enrichMap(7L, "draft-1");
    }
}
