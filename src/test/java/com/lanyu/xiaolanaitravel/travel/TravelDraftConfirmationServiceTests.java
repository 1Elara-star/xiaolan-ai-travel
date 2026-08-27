package com.lanyu.xiaolanaitravel.travel;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftConfirmationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelDraftSessionService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanDraftValidationService;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanItemService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelDraftConfirmationServiceTests {

    @Test
    void adoptsOnlySelectedItemsAndKeepsRemainingDraft() {
        TravelDraftSessionService sessionService =
                mock(TravelDraftSessionService.class);
        TravelPlanItemService planItemService =
                mock(TravelPlanItemService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);
        TravelPlanDraftItem first = item("D1-I1", "鼓浪屿");
        TravelPlanDraftItem second = item("D1-I2", "植物园");
        TravelDraftSession session = session(first, second);
        when(sessionService.getMySession(7L, "draft-1"))
                .thenReturn(session);
        when(planItemService.addFromDraft(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(12L),
                anyList()
        )).thenReturn(List.of());
        when(validationService.validate(session.getDraft())).thenReturn(List.of());

        var response = new TravelDraftConfirmationService(
                sessionService,
                planItemService,
                validationService
        ).confirm(7L, "draft-1", List.of("D1-I2"));

        assertNotNull(response.draftSession());
        assertEquals(1, response.draftSession().draft().getItems().size());
        assertEquals("D1-I1", response.draftSession().draft()
                .getItems().get(0).getDraftItemKey());
        assertEquals(List.of(), response.draftSession().validationIssues());
        verify(planItemService).addFromDraft(
                7L,
                12L,
                List.of(second)
        );
        verify(sessionService, never()).removeSession(7L, "draft-1");
    }

    @Test
    void removesSessionAfterLastCandidateIsAdopted() {
        TravelDraftSessionService sessionService =
                mock(TravelDraftSessionService.class);
        TravelPlanItemService planItemService =
                mock(TravelPlanItemService.class);
        TravelPlanDraftValidationService validationService =
                mock(TravelPlanDraftValidationService.class);
        TravelPlanDraftItem onlyItem = item("D1-I1", "鼓浪屿");
        when(sessionService.getMySession(7L, "draft-1"))
                .thenReturn(session(onlyItem));
        when(planItemService.addFromDraft(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(12L),
                anyList()
        )).thenReturn(List.of());

        var response = new TravelDraftConfirmationService(
                sessionService,
                planItemService,
                validationService
        ).confirm(7L, "draft-1", List.of("D1-I1"));

        assertEquals(null, response.draftSession());
        verify(sessionService).removeSession(7L, "draft-1");
    }

    private TravelDraftSession session(TravelPlanDraftItem... items) {
        TravelPlanDraft draft = new TravelPlanDraft();
        draft.setItems(new ArrayList<>(List.of(items)));
        return new TravelDraftSession(
                "draft-1", 7L, 12L, draft,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(30)
        );
    }

    private TravelPlanDraftItem item(String key, String name) {
        TravelPlanDraftItem item = new TravelPlanDraftItem();
        item.setDraftItemKey(key);
        item.setDayNumber(1);
        item.setItemOrder(Integer.parseInt(key.substring(key.length() - 1)));
        item.setItemType("ATTRACTION");
        item.setPlaceName(name);
        item.setEndDayOffset(0);
        return item;
    }
}
