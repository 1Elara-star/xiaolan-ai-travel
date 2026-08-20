package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.explore.dto.AttractionResponse;
import com.lanyu.xiaolanaitravel.explore.service.ExploreService;
import com.lanyu.xiaolanaitravel.favorite.dto.FavoriteAttractionResponse;
import com.lanyu.xiaolanaitravel.favorite.service.AttractionFavoriteService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSession;
import com.lanyu.xiaolanaitravel.travel.dto.TravelDraftSessionResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 旅行行程生成编排服务。
 *
 * 当前负责：
 *
 * 1. 读取用户已经保存的旅行计划；
 * 2. 读取用户画像和当前目的地收藏；
 * 3. 构造 Prompt；
 * 4. 调用 DeepSeek 生成结构化旅行方案；
 * 5. 将 AI 结果转换为候选 TravelPlanDraft；
 * 6. 使用本地收藏数据补充候选卡片；
 * 7. 创建内存 Draft Session 并返回给前端。
 *
 * 当前流程不会保存正式 TravelPlanItem。
 * 地图真实数据由独立的 Draft 地图补全流程处理。
 */
@Service
public class TravelAiGenerationService {

    private final TravelPlanService travelPlanService;

    private final DeepSeekService deepSeekService;

    private final TravelPlanDraftService travelPlanDraftService;

    private final AttractionFavoriteService favoriteService;

    private final UserProfileService userProfileService;

    private final TravelDraftAttractionService draftAttractionService;

    private final ExploreService exploreService;

    private final TravelDraftSessionService travelDraftSessionService;

    public TravelAiGenerationService(
            TravelPlanService travelPlanService,
            DeepSeekService deepSeekService,
            TravelPlanDraftService travelPlanDraftService,
            AttractionFavoriteService favoriteService,
            UserProfileService userProfileService,
            TravelDraftAttractionService draftAttractionService,
            ExploreService exploreService,
            TravelDraftSessionService travelDraftSessionService) {

        this.travelPlanService = travelPlanService;
        this.deepSeekService = deepSeekService;
        this.travelPlanDraftService = travelPlanDraftService;
        this.favoriteService = favoriteService;
        this.userProfileService = userProfileService;
        this.draftAttractionService = draftAttractionService;
        this.exploreService = exploreService;
        this.travelDraftSessionService = travelDraftSessionService;
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
         * 1. 查询当前用户自己的旅行计划。
         *
         * getMyPlanById() 已经同时完成：
         * - 查询旅行计划
         * - 判断计划是否属于当前用户
         */
        TravelPlan plan =
                travelPlanService.getMyPlanById(
                        userId,
                        planId
                );

        /*
         * 2. 读取用户主动填写的画像，
         * 以及当前目的地相关的收藏景点。
         */
        UserProfile profile =
                userProfileService.getProfile(userId);

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
         * 3. 把旅行需求、画像和收藏整理成 Prompt。
         */
        String prompt =
                buildPrompt(
                        plan,
                        profile,
                        favorites,
                        localAttractions,
                        additionalRequirements
                );

        /*
         * 4. 调用 DeepSeek。
         *
         * DeepSeekService：
         * Prompt
         * → DeepSeek
         * → JSON
         * → AiTravelPlanResponse
         */
        AiTravelPlanResponse aiPlan =
                deepSeekService.generateTravelPlan(
                        prompt
                );

        /*
         * 5. AI DTO → 候选 TravelPlanDraft。
         */
        TravelPlanDraft draft =
                travelPlanDraftService.createDraft(
                        plan,
                        aiPlan
                );

        /*
         * 6. 使用已经存在于本地数据库的收藏景点数据，
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
         * 7. 创建临时候选行程会话。
         *
         * 此时 Draft 只包含 AI 候选内容，
         * 不依赖任何地图接口。
         */
        TravelDraftSession session =
                travelDraftSessionService.createSession(
                        userId,
                        planId,
                        draft
                );

        return new TravelDraftSessionResponse(
                session.getDraftId(),
                session.getExpiresAt(),
                session.getDraft()
        );
    }

    /**
     * 根据 TravelPlan 自动构造旅行规划 Prompt。
     */
    private String buildPrompt(
            TravelPlan plan,
            UserProfile profile,
            List<FavoriteAttractionResponse> favorites,
            List<AttractionResponse> localAttractions,
            String additionalRequirements) {

        return """
                请根据下面已经确认的旅行需求，
                为用户生成一份完整、合理、可执行的旅行行程。

                【旅行基本信息】

                行程标题：%s
                出发城市：%s
                目的地：%s
                开始日期：%s
                结束日期：%s
                旅行天数：%s
                出行人数：%s
                同行类型：%s
                总预算：%s
                旅行类型：%s
                旅行偏好：%s
                特殊要求：%s

                【用户旅行画像】

                MBTI：%s
                旅行节奏：%s
                预算偏好：%s
                交通偏好：%s
                兴趣标签：%s
                不喜欢的内容：%s
                饮食偏好：%s
                住宿偏好：%s

                【当前目的地的收藏景点】

                %s

                【本地景点库中可直接展示资料的景点】

                %s

                【用户对本次候选方案的补充要求】

                %s

                【规划要求】

                1. 必须严格按照旅行天数生成完整行程。
                2. dayNumber 从 1 开始，并且连续排列。
                3. 每天安排合理数量的节点，避免行程过度紧张。
                4. 结合预算、同行情况、旅行类型和旅行偏好进行个性化规划。
                5. startTime 和 endTime 必须使用 HH:mm 格式，例如 09:00、11:30。
                6. 每个节点必须返回 endDayOffset：0表示当天结束，1表示次日结束。
                   例如23:30出发、次日06:30到达时，endDayOffset必须为1，
                   不要为了绕过跨天校验而把结束时间改成null。
                7. 不要编造实时票价、实时酒店价格、精确交通距离和交通耗时。
                8. 在没有真实酒店候选数据时，
                   HOTEL 节点的 placeName 必须填写“待推荐酒店”，
                   不得自行虚构具体酒店。
                9. 当前阶段地点地址、经纬度和真实交通时间由后续地图工具补充，
                   不需要自行猜测。
                10. 行程目的地必须与用户已经确认的旅行计划保持一致。
                11. 收藏景点是高优先级偏好，但要结合天数和旅行节奏选择，
                    不要为了塞入全部收藏而让行程过满。
                12. 使用收藏景点或本地景点库中的景点时，必须返回提供的 attractionId；
                    使用资料中不存在的其他具体地点时 attractionId 填写 null。
                13. ATTRACTION 和 EVENT 必须使用具体、可核验的地点名称，
                    不要使用“海边栈道”“热门街区”“特色景点”等泛化名称。
                """.formatted(
                text(plan.getTitle()),
                text(plan.getDepartureCity()),
                text(plan.getDestination()),
                text(plan.getStartDate()),
                text(plan.getEndDate()),
                text(plan.getTravelDays()),
                text(plan.getPeopleCount()),
                text(plan.getCompanionType()),
                text(plan.getBudget()),
                text(plan.getTripType()),
                text(plan.getTripPreferences()),
                text(plan.getSpecialRequirements()),
                text(profile == null ? null : profile.getMbti()),
                text(profile == null ? null : profile.getTravelPace()),
                text(profile == null ? null : profile.getBudgetPreference()),
                text(profile == null ? null : profile.getTransportPreference()),
                text(profile == null ? null : profile.getInterestTags()),
                text(profile == null ? null : profile.getDislikeTags()),
                text(profile == null ? null : profile.getFoodPreference()),
                text(profile == null ? null : profile.getAccommodationPreference()),
                formatFavorites(favorites),
                formatAttractions(localAttractions),
                text(additionalRequirements)
        );
    }

    private String formatAttractions(
            List<AttractionResponse> attractions) {

        if (attractions == null || attractions.isEmpty()) {
            return "当前目的地暂无本地景点资料";
        }

        return attractions.stream()
                .map(attraction -> "- attractionId=%s｜名称=%s｜类型=%s｜特色=%s｜建议时长=%s｜标签=%s"
                        .formatted(
                                attraction.id(),
                                text(attraction.name()),
                                text(attraction.type()),
                                text(attraction.popularReason()),
                                text(attraction.suggestedDuration()),
                                attraction.tags() == null
                                        || attraction.tags().isEmpty()
                                        ? "未填写"
                                        : String.join("、", attraction.tags())
                        ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("当前目的地暂无本地景点资料");
    }

    private String formatFavorites(
            List<FavoriteAttractionResponse> favorites) {

        if (favorites == null || favorites.isEmpty()) {
            return "当前目的地暂无收藏景点";
        }

        return favorites.stream()
                .map(FavoriteAttractionResponse::attraction)
                .map(attraction -> "- attractionId=%s｜名称=%s｜类型=%s｜特色=%s｜建议时长=%s｜标签=%s"
                        .formatted(
                                attraction.id(),
                                text(attraction.name()),
                                text(attraction.type()),
                                text(attraction.popularReason()),
                                text(attraction.suggestedDuration()),
                                attraction.tags() == null
                                        || attraction.tags().isEmpty()
                                        ? "未填写"
                                        : String.join("、", attraction.tags())
                        ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("当前目的地暂无收藏景点");
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

    /**
     * Prompt 中不直接出现 null。
     */
    private String text(
            Object value) {

        if (value == null) {
            return "未填写";
        }

        String result =
                value.toString().strip();

        return result.isBlank()
                ? "未填写"
                : result;
    }
}
