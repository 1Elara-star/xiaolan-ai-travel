package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelItem;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraft;
import com.lanyu.xiaolanaitravel.travel.dto.TravelPlanDraftItem;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 候选行程构建服务。
 *
 * 负责：
 * 1. 校验 DeepSeek 返回的结构化行程；
 * 2. 将 AiTravelPlanResponse 转换为 TravelPlanDraft；
 * 3. 为候选节点生成 itemOrder 和 draftItemKey；
 * 4. 将 AI 返回的字符串时间转换成 LocalTime。
 *
 * 本服务不访问数据库，
 * 不调用高德，
 * 也不保存正式 TravelPlanItem。
 */
@Service
public class TravelPlanDraftService {

    /**
     * 系统允许的行程节点类型。
     */
    private static final Set<String> ALLOWED_ITEM_TYPES = Set.of(
            "ATTRACTION",
            "FOOD",
            "HOTEL",
            "EVENT",
            "REST",
            "OTHER"
    );

    /**
     * 将 DeepSeek 返回的 AI 行程转换成系统内部候选方案。
     */
    public TravelPlanDraft createDraft(
            TravelPlan plan,
            AiTravelPlanResponse aiPlan) {

        if (plan == null) {
            throw new IllegalArgumentException(
                    "旅行计划不能为空"
            );
        }

        /*
         * 第一层：
         * 检查 AI 返回的数据结构是否合法。
         */
        validateAiPlan(
                aiPlan,
                plan.getTravelDays()
        );

        /*
         * 第二层：
         * 创建整份候选方案。
         *
         * planId、destination、travelDays
         * 使用数据库 TravelPlan 中已经确认的数据，
         * 不直接相信 AI 返回的业务事实。
         */
        TravelPlanDraft draft =
                new TravelPlanDraft();

        draft.setPlanId(plan.getId());
        draft.setDestination(plan.getDestination());
        draft.setTravelDays(plan.getTravelDays());
        draft.setSummary(
                normalize(aiPlan.getSummary())
        );

        List<TravelPlanDraftItem> draftItems =
                new ArrayList<>();

        /*
         * 对 AI 返回的 days 做一个副本并按 dayNumber 排序。
         *
         * 即使模型返回顺序偶尔变成：
         * Day2、Day1、Day3，
         *
         * 后端生成的 Draft 仍然保持：
         * Day1、Day2、Day3。
         */
        List<AiTravelDay> sortedDays =
                new ArrayList<>(aiPlan.getDays());

        sortedDays.sort(
                Comparator.comparing(
                        AiTravelDay::getDayNumber
                )
        );

        /*
         * 第三层：
         * 将每一天、每一个 AI 节点
         * 转换成候选 DraftItem。
         */
        for (AiTravelDay day : sortedDays) {

            int itemOrder = 1;

            for (AiTravelItem aiItem : day.getItems()) {

                TravelPlanDraftItem draftItem =
                        new TravelPlanDraftItem();

                /*
                 * 后端生成的候选节点身份。
                 *
                 * 例如：
                 * D1-I1
                 * D1-I2
                 * D2-I1
                 */
                draftItem.setDayNumber(
                        day.getDayNumber()
                );

                draftItem.setItemOrder(
                        itemOrder
                );

                draftItem.setDraftItemKey(
                        buildDraftItemKey(
                                day.getDayNumber(),
                                itemOrder
                        )
                );

                draftItem.setAttractionId(
                        aiItem.getAttractionId()
                );
                draftItem.setSourceType(
                        "AI_RECOMMENDED"
                );

                /*
                 * DeepSeek 提供的规划内容。
                 */
                draftItem.setItemType(
                        aiItem.getItemType()
                );

                draftItem.setPlaceName(
                        aiItem.getPlaceName().strip()
                );

                draftItem.setStartTime(
                        parseTime(
                                aiItem.getStartTime()
                        )
                );

                draftItem.setEndTime(
                        parseTime(
                                aiItem.getEndTime()
                        )
                );

                draftItem.setEndDayOffset(
                        aiItem.getEndDayOffset()
                );

                draftItem.setDescription(
                        normalize(
                                aiItem.getDescription()
                        )
                );

                /*
                 * 高德和路线字段目前保持 null。
                 *
                 * 下一阶段 TravelDraftMapService
                 * 才会负责补充这些真实世界数据。
                 */
                draftItem.setPoiId(null);
                draftItem.setMatchedPoiName(null);
                draftItem.setAddress(null);
                draftItem.setLongitude(null);
                draftItem.setLatitude(null);
                draftItem.setCityCode(null);

                draftItem.setTransportMode(null);
                draftItem.setDistanceFromPrev(null);
                draftItem.setTravelTimeFromPrev(null);

                draftItems.add(draftItem);

                itemOrder++;
            }
        }

        draft.setItems(draftItems);

        return draft;
    }

    /**
     * 校验整份 AI 行程的结构。
     *
     * 这里校验的是：
     * “DeepSeek 有没有按照我们约定的数据契约返回结果。”
     *
     * 不是检查：
     * “这个旅行安排在真实世界里合不合理。”
     *
     * 后一种检查以后交给 TravelPlanValidationService。
     */
    private void validateAiPlan(
            AiTravelPlanResponse aiPlan,
            Integer travelDays) {

        if (aiPlan == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI没有返回有效的旅行计划"
            );
        }

        if (travelDays == null || travelDays < 1) {
            throw new IllegalArgumentException(
                    "原旅行计划缺少有效的旅行天数"
            );
        }

        /*
         * AI 自己声明的旅行天数
         * 必须和用户原旅行计划一致。
         */
        if (aiPlan.getTravelDays() == null
                || !aiPlan.getTravelDays()
                .equals(travelDays)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的旅行天数与原旅行计划不一致"
            );
        }

        if (aiPlan.getDays() == null
                || aiPlan.getDays().size() != travelDays) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的每日行程数量与旅行天数不一致"
            );
        }

        Set<Integer> dayNumbers =
                new HashSet<>();

        for (AiTravelDay day : aiPlan.getDays()) {

            if (day == null
                    || day.getDayNumber() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程缺少 dayNumber"
                );
            }

            if (day.getDayNumber() < 1
                    || day.getDayNumber() > travelDays) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程天数超出旅行计划范围"
                );
            }

            if (!dayNumbers.add(
                    day.getDayNumber())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回了重复的行程天数"
                );
            }

            if (day.getItems() == null
                    || day.getItems().isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的某一天没有行程节点"
                );
            }

            for (AiTravelItem item :
                    day.getItems()) {

                validateAiItem(item);
            }
        }

        /*
         * 检查有没有漏天。
         *
         * 正常必须：
         * 1、2、3、...、travelDays
         * 全部存在。
         */
        for (int dayNumber = 1;
             dayNumber <= travelDays;
             dayNumber++) {

            if (!dayNumbers.contains(dayNumber)) {

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
     * 校验单个 AI 行程节点。
     */
    private void validateAiItem(
            AiTravelItem item) {

        if (item == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回了空的行程节点"
            );
        }

        if (item.getPlaceName() == null
                || item.getPlaceName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的行程节点缺少地点名称"
            );
        }

        if (item.getItemType() == null
                || !ALLOWED_ITEM_TYPES.contains(
                item.getItemType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回了不支持的行程节点类型"
            );
        }

        LocalTime startTime =
                parseTime(item.getStartTime());

        LocalTime endTime =
                parseTime(item.getEndTime());

        if (item.getEndDayOffset() == null
                || (item.getEndDayOffset() != 0
                && item.getEndDayOffset() != 1)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的endDayOffset必须是0或1"
            );
        }

        /*
         * 跨天节点必须同时有开始时间和结束时间。
         */
        if (item.getEndDayOffset() == 1
                && (startTime == null
                || endTime == null)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的跨天节点必须同时包含开始时间和结束时间"
            );
        }

        /*
         * 当天结束的节点：
         *
         * 09:00 → 11:00 合法
         * 11:00 → 09:00 非法
         *
         * 跨天节点不执行这一条检查。
         */
        if (startTime != null
                && endTime != null
                && item.getEndDayOffset() == 0
                && !endTime.isAfter(startTime)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的当天行程节点结束时间必须晚于开始时间"
            );
        }
    }

    /**
     * 将 AI 返回的字符串时间转换成 LocalTime。
     *
     * 例如：
     * "09:30" → LocalTime(09:30)
     */
    private LocalTime parseTime(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(
                    value.strip()
            );

        } catch (DateTimeParseException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的时间格式不正确："
                            + value
            );
        }
    }

    /**
     * 生成候选节点临时 ID。
     *
     * 例如：
     * dayNumber = 2
     * itemOrder = 3
     *
     * 结果：
     * D2-I3
     */
    private String buildDraftItemKey(
            int dayNumber,
            int itemOrder) {

        return "D"
                + dayNumber
                + "-I"
                + itemOrder;
    }

    /**
     * 空字符串统一转换为 null。
     */
    private String normalize(
            String value) {

        return value == null
                || value.isBlank()
                ? null
                : value.strip();
    }
}
