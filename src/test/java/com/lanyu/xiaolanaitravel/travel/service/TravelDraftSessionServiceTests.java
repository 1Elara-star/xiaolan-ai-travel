package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TravelDraftSessionServiceTests {

    @Test
    void createsSessionWithThirtyMinuteExpiration() {
        MutableClock clock = clock();
        TravelDraftSessionService service =
                new TravelDraftSessionService(clock);
        TravelPlanDraft draft = new TravelPlanDraft();

        TravelDraftSession session =
                service.createSession(7L, 12L, draft);

        assertNotNull(session.getDraftId());
        assertEquals(7L, session.getUserId());
        assertEquals(12L, session.getPlanId());
        assertSame(draft, session.getDraft());
        assertEquals(
                LocalDateTime.of(2026, 8, 19, 12, 0),
                session.getCreatedAt()
        );
        assertEquals(
                session.getCreatedAt().plusMinutes(30),
                session.getExpiresAt()
        );
        assertSame(
                session,
                service.getMySession(7L, session.getDraftId())
        );
    }

    @Test
    void rejectsSessionOwnedByAnotherUser() {
        TravelDraftSessionService service =
                new TravelDraftSessionService(clock());
        TravelDraftSession session =
                service.createSession(7L, 12L, new TravelPlanDraft());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMySession(8L, session.getDraftId())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void expiredSessionCannotBeRead() {
        MutableClock clock = clock();
        TravelDraftSessionService service =
                new TravelDraftSessionService(clock);
        TravelDraftSession session =
                service.createSession(7L, 12L, new TravelPlanDraft());
        clock.advance(Duration.ofMinutes(30));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getMySession(7L, session.getDraftId())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void removedSessionCannotBeRead() {
        TravelDraftSessionService service =
                new TravelDraftSessionService(clock());
        TravelDraftSession session =
                service.createSession(7L, 12L, new TravelPlanDraft());

        service.removeSession(7L, session.getDraftId());

        assertThrows(
                ResponseStatusException.class,
                () -> service.getMySession(7L, session.getDraftId())
        );
    }

    private MutableClock clock() {
        return new MutableClock(
                Instant.parse("2026-08-19T12:00:00Z"),
                ZoneOffset.UTC
        );
    }

    private static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(
                Instant instant,
                ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
