package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.ai.service.AiTravelPlanService;
import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

/**
 * AI 旅行行程生成编排服务。
 *
 * 当前负责：
 *
 * 1. 读取用户已经保存的旅行计划；
 * 2. 根据旅行计划构造 Prompt；
 * 3. 调用 DeepSeek 生成结构化旅行方案；
 * 4. 将 AI 结果转换为候选 TravelPlanDraft；
 * 5. 当前阶段仍沿用原来的持久化逻辑保存正式行程。
 *
 * 后续会继续在 Draft 和最终持久化之间加入：
 * 高德数据补全、路线计算、固定 Workflow 校验和 Repair。
 */
@Service
public class TravelAiGenerationService {

    private final TravelPlanService travelPlanService;

    private final DeepSeekService deepSeekService;

    private final TravelPlanDraftService travelPlanDraftService;

    private final AiTravelPlanService aiTravelPlanService;

    public TravelAiGenerationService(
            TravelPlanService travelPlanService,
            DeepSeekService deepSeekService,
            TravelPlanDraftService travelPlanDraftService,
            AiTravelPlanService aiTravelPlanService) {

        this.travelPlanService = travelPlanService;
        this.deepSeekService = deepSeekService;
        this.travelPlanDraftService = travelPlanDraftService;
        this.aiTravelPlanService = aiTravelPlanService;
    }

    /**
     * 根据指定旅行计划生成 AI 行程，并保存到数据库。
     */
    public AiTravelPlanResponse generateAndSave(
            Long userId,
            Long planId) {

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
         * 2. 把数据库里的旅行需求整理成 Prompt。
         */
        String prompt =
                buildPrompt(plan);

        /*
         * 3. 调用 DeepSeek。
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
         * 4. 保留当前已有的整体业务校验。
         *
         * 目前先不删除，
         * 等新的 Draft 流程稳定以后，
         * 再统一清理重复校验。
         */
        validateGeneratedPlan(
                plan,
                aiPlan
        );

        /*
         * 5. AI DTO → 候选 TravelPlanDraft。
         *
         * 从这里开始，
         * AI 输出已经不再直接等同于正式数据库行程。
         *
         * 下一阶段会在这里继续加入：
         *
         * Draft
         * → 高德 POI
         * → 路线
         * → Workflow 校验
         * → Repair
         * → 最终保存
         */
        TravelPlanDraft draft =
                travelPlanDraftService.createDraft(
                        plan,
                        aiPlan
                );

        /*
         * 当前阶段暂时还没有让 Draft 进入高德和 Workflow。
         *
         * 所以为了不破坏现有业务接口，
         * 仍然沿用原来的持久化逻辑。
         *
         * draft 现在虽然暂时没有继续使用，
         * 但它已经正式进入主业务链路。
         */
        aiTravelPlanService.saveGeneratedPlan(
                userId,
                planId,
                aiPlan
        );

        /*
         * 当前 Controller 仍然返回原始 AI 结构。
         *
         * 暂时不修改接口返回类型，
         * 避免这一小步影响前端或现有调用。
         */
        return aiPlan;
    }

    /**
     * 根据 TravelPlan 自动构造旅行规划 Prompt。
     */
    private String buildPrompt(
            TravelPlan plan) {

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
                text(plan.getSpecialRequirements())
        );
    }

    /**
     * 校验 AI 返回的整份行程是否符合原旅行计划。
     *
     * 当前暂时保留，
     * 后续 Draft 流程稳定后再去除重复校验。
     */
    private void validateGeneratedPlan(
            TravelPlan plan,
            AiTravelPlanResponse aiPlan) {

        if (aiPlan == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI没有返回有效的旅行计划"
            );
        }

        if (aiPlan.getTravelDays() == null
                || !aiPlan.getTravelDays()
                .equals(plan.getTravelDays())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的旅行天数与原旅行计划不一致"
            );
        }

        if (aiPlan.getDays() == null
                || aiPlan.getDays().size()
                != plan.getTravelDays()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的每日行程数量与旅行天数不一致"
            );
        }

        Set<Integer> dayNumbers =
                new HashSet<>();

        for (AiTravelDay day :
                aiPlan.getDays()) {

            if (day == null
                    || day.getDayNumber() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程缺少 dayNumber"
                );
            }

            if (!dayNumbers.add(
                    day.getDayNumber())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回了重复的行程天数"
                );
            }
        }

        for (int dayNumber = 1;
             dayNumber <= plan.getTravelDays();
             dayNumber++) {

            if (!dayNumbers.contains(
                    dayNumber)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程缺少第 "
                                + dayNumber
                                + " 天"
                );
            }
        }
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