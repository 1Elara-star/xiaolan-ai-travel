package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 候选行程临时会话服务。
 *
 * 第一版只保存在当前服务器内存中，
 * 不访问数据库，也不依赖 Redis。
 */
@Service
public class TravelDraftSessionService {

    private static final int SESSION_DURATION_MINUTES = 30;

    private final Map<String, TravelDraftSession> sessions =
            new ConcurrentHashMap<>();

    private final Clock clock;

    public TravelDraftSessionService() {
        this(Clock.systemDefaultZone());
    }

    TravelDraftSessionService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 创建一个有效期为 30 分钟的候选行程会话。
     */
    public TravelDraftSession createSession(
            Long userId,
            Long planId,
            TravelPlanDraft draft) {

        if (userId == null || planId == null || draft == null) {
            throw new IllegalArgumentException(
                    "创建候选行程会话缺少必要数据"
            );
        }

        LocalDateTime createdAt =
                LocalDateTime.now(clock);

        removeExpiredSessions(createdAt);

        TravelDraftSession session =
                new TravelDraftSession(
                        UUID.randomUUID().toString(),
                        userId,
                        planId,
                        draft,
                        createdAt,
                        createdAt.plusMinutes(
                                SESSION_DURATION_MINUTES
                        )
                );

        sessions.put(
                session.getDraftId(),
                session
        );

        return session;
    }

    /**
     * 读取当前用户自己的有效候选行程会话。
     */
    public TravelDraftSession getMySession(
            Long userId,
            String draftId) {

        TravelDraftSession session =
                findSession(draftId);

        LocalDateTime now =
                LocalDateTime.now(clock);

        if (session == null) {
            throw sessionNotFound();
        }

        if (isExpired(session, now)) {
            sessions.remove(
                    draftId,
                    session
            );
            throw sessionNotFound();
        }

        if (!Objects.equals(
                session.getUserId(),
                userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "无权访问该候选行程会话"
            );
        }

        return session;
    }

    /**
     * 删除当前用户自己的候选行程会话。
     */
    public void removeSession(
            Long userId,
            String draftId) {

        TravelDraftSession session =
                getMySession(userId, draftId);

        sessions.remove(
                draftId,
                session
        );
    }

    private TravelDraftSession findSession(
            String draftId) {

        if (draftId == null || draftId.isBlank()) {
            return null;
        }

        return sessions.get(draftId);
    }

    private void removeExpiredSessions(
            LocalDateTime now) {

        sessions.entrySet().removeIf(
                entry -> isExpired(
                        entry.getValue(),
                        now
                )
        );
    }

    private boolean isExpired(
            TravelDraftSession session,
            LocalDateTime now) {

        return !now.isBefore(
                session.getExpiresAt()
        );
    }

    private ResponseStatusException sessionNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "候选行程会话不存在或已过期"
        );
    }
}
