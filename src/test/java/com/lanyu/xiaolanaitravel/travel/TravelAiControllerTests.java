package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.controller.TravelAiController;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.service.TravelAiGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelAiControllerTests {

    @Test
    void returnsCreatedDraftSessionResponse() {
        TravelAiGenerationService generationService =
                mock(TravelAiGenerationService.class);
        TravelDraftSessionResponse expected =
                new TravelDraftSessionResponse(
                        "draft-1",
                        LocalDateTime.of(2026, 8, 19, 12, 30),
                        new TravelPlanDraft(),
                        List.of(),
                        false
                );
        when(generationService.generateDraftSession(7L, 12L, null))
                .thenReturn(expected);

        var response = new TravelAiController(generationService)
                .generateTravelPlan(7L, 12L, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(generationService).generateDraftSession(7L, 12L, null);
    }
}
