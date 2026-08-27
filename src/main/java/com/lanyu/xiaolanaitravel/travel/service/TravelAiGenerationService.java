package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowResponse;
import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowService;
import com.lanyu.xiaolanaitravel.ai.agent.planner.PlannerWorkflowStatus;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelValidationIssue;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 旅行行程生成编排服务。
 *
 * 当前负责：
 *
 * 1. 运行受控 Planner Workflow；
 * 2. Planner 综合旅行计划、画像、已确认记忆、收藏和本次补充要求；
 * 3. 按需调用白名单 Tool，并把事实持续交回 Planner；
 * 4. Planner 返回最终结构化旅行方案；
 * 5. 将 AI 结果转换为候选 TravelPlanDraft；
 * 6. 使用本地景点资料补充候选卡片；
 * 7. 执行本地固定 Workflow 校验；
 * 8. 创建内存 Draft Session，并将 Draft 与校验问题返回前端。
 *
 * 当前流程不会保存正式 TravelPlanItem。
 * 地图真实数据由独立的 Draft 地图补全流程处理。
 */
@Service
public class TravelAiGenerationService {

    private final TravelPlanService travelPlanService;

    private final PlannerWorkflowService plannerWorkflowService;

    private final TravelPlanDraftService travelPlanDraftService;

    private final AttractionFavoriteService favoriteService;

    private final TravelDraftAttractionService draftAttractionService;

    private final ExploreService exploreService;

    private final TravelDraftSessionService travelDraftSessionService;

    private final TravelPlanDraftValidationService draftValidationService;

    public TravelAiGenerationService(
            TravelPlanService travelPlanService,
            PlannerWorkflowService plannerWorkflowService,
            TravelPlanDraftService travelPlanDraftService,
            AttractionFavoriteService favoriteService,
            TravelDraftAttractionService draftAttractionService,
            ExploreService exploreService,
            TravelDraftSessionService travelDraftSessionService,
            TravelPlanDraftValidationService draftValidationService) {

        this.travelPlanService = travelPlanService;
        this.plannerWorkflowService = plannerWorkflowService;
        this.travelPlanDraftService = travelPlanDraftService;
        this.favoriteService = favoriteService;
        this.draftAttractionService = draftAttractionService;
        this.exploreService = exploreService;
        this.travelDraftSessionService = travelDraftSessionService;
        this.draftValidationService = draftValidationService;
    }

    /**
     * 根据指定旅行计划生成候选行程会话。
     */
    public TravelDraftSessionResponse generateDraftSession(
            Long userId,
            Long planId) {

        return generateDraftSession(userId, planId, null);
    }

    /**
     * 根据指定旅行计划和本次补充要求生成候选行程会话。
     */
    public TravelDraftSessionResponse generateDraftSession(
            Long userId,
            Long planId,
            String additionalRequirements) {

        /*
         * 1. 运行受控 Planner Workflow。
         * PlannerContextService 会在 Workflow 内读取当前用户自己的旅行计划、
         * 用户画像、已确认记忆和收藏。每次 Tool 结果都会重新交回 Planner。
         */
        PlannerWorkflowResponse workflow = plannerWorkflowService.run(
                userId,
                planId,
                buildPlannerRequest(additionalRequirements)
        );

        if (workflow.status() != PlannerWorkflowStatus.COMPLETED
                || workflow.finalPlan() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    workflow.errorMessage() == null
                            ? "Planner尚未生成完整候选行程"
                            : workflow.errorMessage()
            );
        }

        AiTravelPlanResponse aiPlan = workflow.finalPlan();

        /*
         * 2. 再读取一次受权限保护的 TravelPlan，供 Draft 转换和本地景点补充使用。
         */
        TravelPlan plan = travelPlanService.getMyPlanById(userId, planId);

        List<FavoriteAttractionResponse> favorites =
                favoriteService.list(userId).stream()
                        .filter(favorite -> isDestinationFavorite(
                                favorite,
                                plan.getDestination()
                        ))
                        .toList();

        List<AttractionResponse> localAttractions =
                exploreService.listAttractions(
                        plan.getDestination(),
                        null,
                        null
                );

        /*
         * 3. Planner 最终 DTO → 候选 TravelPlanDraft。
         */
        TravelPlanDraft draft =
                travelPlanDraftService.createDraft(
                        plan,
                        aiPlan
                );

        /*
         * 4. 使用已经存在于本地数据库的景点数据，
         * 补充图片、故事、地址和已有坐标。
         * 这里不调用高德。
         */
        Set<Long> favoriteIds = favorites.stream()
                .map(FavoriteAttractionResponse::attraction)
                .map(AttractionResponse::id)
                .collect(Collectors.toSet());
        draftAttractionService.enrichFromCatalog(
                draft,
                localAttractions,
                favoriteIds
        );

        /*
         * 5. 先执行不依赖外部服务的固定 Workflow 校验，
         * 再创建临时候选行程会话。
         *
         * 此时 Draft 只包含 AI 候选内容，
         * 不依赖任何地图接口。
         */
        List<TravelValidationIssue> validationIssues =
                draftValidationService.validate(draft);

        TravelDraftSession session =
                travelDraftSessionService.createSession(
                        userId,
                        planId,
                        draft
                );

        return new TravelDraftSessionResponse(
                session.getDraftId(),
                session.getExpiresAt(),
                session.getDraft(),
                validationIssues,
                draftValidationService.hasErrors(validationIssues)
        );
    }

    private String buildPlannerRequest(String additionalRequirements) {
        String baseRequest = "请综合当前旅行计划、用户画像、已确认记忆、收藏景点和必要的外部真实事实，生成完整候选行程。";
        if (additionalRequirements == null || additionalRequirements.isBlank()) {
            return baseRequest;
        }
        return baseRequest + "\n【本次补充要求】\n" + additionalRequirements.strip();
    }

    private boolean isDestinationFavorite(
            FavoriteAttractionResponse favorite,
            String destination) {

        if (favorite == null || favorite.attraction() == null) {
            return false;
        }

        AttractionResponse attraction = favorite.attraction();
        String favoriteCity = normalizeCity(attraction.city());
        String planDestination = normalizeCity(destination);

        return !favoriteCity.isBlank()
                && !planDestination.isBlank()
                && (favoriteCity.equals(planDestination)
                || favoriteCity.contains(planDestination)
                || planDestination.contains(favoriteCity));
    }

    private String normalizeCity(String value) {
        if (value == null) {
            return "";
        }

        return value.strip()
                .replaceFirst("(特别行政区|自治州|自治区|市)$", "");
    }

}
