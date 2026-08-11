package com.lanyu.xiaolanaitravel.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlanItem;
import com.lanyu.xiaolanaitravel.travel.mapper.TravelPlanItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 旅行行程节点业务逻辑。
 */
@Service
public class TravelPlanItemService {

    private final TravelPlanItemMapper travelPlanItemMapper;
    private final TravelPlanService travelPlanService;

    public TravelPlanItemService(
            TravelPlanItemMapper travelPlanItemMapper,
            TravelPlanService travelPlanService) {
        this.travelPlanItemMapper = travelPlanItemMapper;
        this.travelPlanService = travelPlanService;
    }

    /**
     * 查询当前登录用户某个旅行计划下的全部行程节点。
     *
     * <p>先校验旅行计划归属，再查询节点，避免通过 planId
     * 读取其他用户的私人行程。</p>
     */
    public List<TravelPlanItem> getMyPlanItems(Long userId, Long planId) {
        travelPlanService.getMyPlanById(userId, planId);

        LambdaQueryWrapper<TravelPlanItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelPlanItem::getPlanId, planId)
                .orderByAsc(TravelPlanItem::getDayNumber)
                .orderByAsc(TravelPlanItem::getItemOrder)
                .orderByAsc(TravelPlanItem::getId);

        return travelPlanItemMapper.selectList(wrapper);
    }
}
