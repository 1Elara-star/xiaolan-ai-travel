package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import org.springframework.stereotype.Service;

/**
 * 对已经存在的候选行程 Session 补充地图事实数据。
 *
 * 该服务不调用 DeepSeek，也不保存正式 TravelPlanItem。
 */
@Service
public class TravelDraftEnrichmentService {

    private final TravelDraftSessionService sessionService;
    private final TravelDraftMapService mapService;

    public TravelDraftEnrichmentService(
            TravelDraftSessionService sessionService,
            TravelDraftMapService mapService) {
        this.sessionService = sessionService;
        this.mapService = mapService;
    }

    /**
     * 为当前用户自己的有效 Draft 补充 POI 数据和直线距离预览。
     *
     * TravelDraftMapService 会直接修改 Session 中保存的同一个 Draft 对象，
     * 因此补全结果会保留在当前内存 Session 中。
     */
    public TravelDraftSessionResponse enrichMap(
            Long userId,
            String draftId) {

        TravelDraftSession session =
                sessionService.getMySession(
                        userId,
                        draftId
                );

        TravelPlanDraft draft =
                session.getDraft();

        mapService.enrichLocations(draft);
        mapService.enrichStraightLineDistances(draft);

        return new TravelDraftSessionResponse(
                session.getDraftId(),
                session.getExpiresAt(),
                draft
        );
    }
}
