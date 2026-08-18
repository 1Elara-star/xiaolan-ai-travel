package com.lanyu.xiaolanaitravel.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelDay;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelItem;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import com.lanyu.xiaolanaitravel.travel.service.TravelPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * AI 行程结果持久化业务。
 *
 * 负责将 DeepSeek 返回的结构化行程，
 * 转换为正式的 TravelPlanItem 并保存到数据库。
 */
@Service
public class AiTravelPlanService {

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

    private final TravelPlanService travelPlanService;
    private final TravelPlanItemMapper travelPlanItemMapper;

    public AiTravelPlanService(
            TravelPlanService travelPlanService,
            TravelPlanItemMapper travelPlanItemMapper) {

        this.travelPlanService = travelPlanService;
        this.travelPlanItemMapper = travelPlanItemMapper;
    }

    /**
     * 将 AI 生成的行程保存为正式行程节点。
     */
    @Transactional
    public void saveGeneratedPlan(
            Long userId,
            Long planId,
            AiTravelPlanResponse aiPlan) {

        /*
         * 1. 查询当前用户自己的旅行计划。
         *
         * 这里除了获得旅行天数，
         * 也同时完成权限校验：
         * 当前用户不能给别人的旅行计划生成行程。
         */
        var plan = travelPlanService.getMyPlanById(userId, planId);

        /*
         * 2. 检查 AI 返回的数据是否合法。
         */
        validateAiPlan(aiPlan, plan.getTravelDays());

        /*
         * 3. 检查这个计划是不是已经存在正式行程节点。
         *
         * 当前版本不允许 AI 静默覆盖已有行程。
         */
        Long existingCount = travelPlanItemMapper.selectCount(
                new LambdaQueryWrapper<TravelPlanItem>()
                        .eq(TravelPlanItem::getPlanId, planId)
        );

        if (existingCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "当前旅行计划已经存在行程节点，暂不自动覆盖已有行程"
            );
        }

        /*
         * 4. 遍历每一天。
         */
        for (AiTravelDay day : aiPlan.getDays()) {

            /*
             * 每一天的节点顺序都从 1 开始。
             *
             * itemOrder 不让 AI 生成，
             * 而是由后端根据返回顺序控制。
             */
            int itemOrder = 1;

            /*
             * 5. 遍历当天的每一个 AI 行程节点。
             */
            for (AiTravelItem aiItem : day.getItems()) {

                TravelPlanItem item = new TravelPlanItem();

                /*
                 * 后端控制的业务字段。
                 */
                item.setPlanId(planId);
                item.setDayNumber(day.getDayNumber());
                item.setItemOrder(itemOrder++);

                /*
                 * DeepSeek 提供的行程内容。
                 */
                item.setItemType(aiItem.getItemType());
                item.setPlaceName(aiItem.getPlaceName().strip());
                item.setStartTime(parseTime(aiItem.getStartTime()));
                item.setEndTime(parseTime(aiItem.getEndTime()));
                item.setEndDayOffset(aiItem.getEndDayOffset());
                item.setDescription(normalize(aiItem.getDescription()));

                /*
                 * 这些字段暂时不让 DeepSeek 编造。
                 *
                 * address / longitude / latitude：
                 * 后续由高德提供真实数据。
                 *
                 * distanceFromPrev / travelTimeFromPrev：
                 * 后续由高德路线规划计算。
                 *
                 * attractionId：
                 * 后续和系统景点数据匹配后再补充。
                 */
                item.setAttractionId(null);
                item.setAddress(null);
                item.setLongitude(null);
                item.setLatitude(null);
                item.setTransportMode(null);
                item.setDistanceFromPrev(null);
                item.setTravelTimeFromPrev(null);

                /*
                 * 6. 保存数据库。
                 */
                if (travelPlanItemMapper.insert(item) == 0) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "AI行程节点保存失败"
                    );
                }
            }
        }
    }

    /**
     * 校验整份 AI 行程。
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

        if (aiPlan.getDays() == null || aiPlan.getDays().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的旅行计划没有每日行程"
            );
        }

        for (AiTravelDay day : aiPlan.getDays()) {

            if (day == null || day.getDayNumber() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程缺少天数信息"
                );
            }

            if (day.getDayNumber() < 1
                    || day.getDayNumber() > travelDays) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的行程天数超出旅行计划范围"
                );
            }

            if (day.getItems() == null || day.getItems().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI返回的某一天没有行程节点"
                );
            }

            for (AiTravelItem item : day.getItems()) {
                validateAiItem(item);
            }
        }
    }

    /**
     * 校验单个 AI 行程节点。
     */
    private void validateAiItem(AiTravelItem item) {

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
                || !ALLOWED_ITEM_TYPES.contains(item.getItemType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回了不支持的行程节点类型"
            );
        }

        LocalTime startTime = parseTime(item.getStartTime());
        LocalTime endTime = parseTime(item.getEndTime());

        if (item.getEndDayOffset() == null
                || (item.getEndDayOffset() != 0 && item.getEndDayOffset() != 1)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的endDayOffset必须是0或1"
            );
        }

        if (item.getEndDayOffset() == 1
                && (startTime == null || endTime == null)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的跨天节点必须同时包含开始时间和结束时间"
            );
        }

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
     * 把 AI 返回的字符串时间，例如 "09:00"，
     * 转换成 TravelPlanItem 需要的 LocalTime。
     */
    private LocalTime parseTime(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(value.strip());

        } catch (DateTimeParseException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的时间格式不正确：" + value
            );
        }
    }

    /**
     * 空字符串统一转换成 null。
     */
    private String normalize(String value) {

        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
