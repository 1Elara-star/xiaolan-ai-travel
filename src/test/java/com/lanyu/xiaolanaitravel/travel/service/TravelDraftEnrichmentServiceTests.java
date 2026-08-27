package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelValidationIssue;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelDraftEnrichmentServiceTests {

    @Test
    void enrichesSessionDraftAndReturnsUpdatedSessionResponse() {
        TravelDraftSessionService sessionService =
                mock(TravelDraftSessionService.class);
        TravelDraftMapService mapService =
                mock(TravelDraftMapService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);
        TravelPlanDraft draft = new TravelPlanDraft();
        LocalDateTime expiresAt =
                LocalDateTime.of(2026, 8, 20, 10, 30);
        TravelDraftSession session = new TravelDraftSession(
                "draft-1",
                7L,
                12L,
                draft,
                expiresAt.minusMinutes(30),
                expiresAt
        );

        when(sessionService.getMySession(7L, "draft-1"))
                .thenReturn(session);
        when(mapService.enrichLocations(draft))
                .thenReturn(draft);
        when(mapService.enrichStraightLineDistances(draft))
                .thenReturn(draft);
        List<TravelValidationIssue> issues = List.of(
                new TravelValidationIssue(
                        "POI_COORDINATES_MISSING",
                        "WARNING",
                        "D1-I1",
                        null,
                        "地点缺少经纬度"
                )
        );
        when(validationService.validate(draft)).thenReturn(issues);
        when(validationService.hasErrors(issues)).thenReturn(false);

        TravelDraftSessionResponse response =
                new TravelDraftEnrichmentService(
                        sessionService,
                        mapService,
                        validationService
                ).enrichMap(7L, "draft-1");

        assertEquals("draft-1", response.draftId());
        assertEquals(expiresAt, response.expiresAt());
        assertSame(draft, response.draft());
        assertSame(issues, response.validationIssues());
        assertEquals(false, response.hasErrors());

        var order = inOrder(sessionService, mapService, validationService);
        order.verify(sessionService)
                .getMySession(7L, "draft-1");
        order.verify(mapService)
                .enrichLocations(draft);
        order.verify(mapService)
                .enrichStraightLineDistances(draft);
        order.verify(validationService).validate(draft);
    }

    @Test
    void rejectsForeignSessionBeforeCallingMapService() {
        TravelDraftSessionService sessionService =
                mock(TravelDraftSessionService.class);
        TravelDraftMapService mapService =
                mock(TravelDraftMapService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);
        when(sessionService.getMySession(8L, "draft-1"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "无权访问该候选行程会话"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new TravelDraftEnrichmentService(
                        sessionService,
                        mapService,
                        validationService
                ).enrichMap(8L, "draft-1")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(mapService);
        verifyNoInteractions(validationService);
    }
}
