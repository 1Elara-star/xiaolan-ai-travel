package com.lanyu.xiaolanaitravel.ai.agent.planner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** 把当前用户的真实旅行上下文交给 Planner Agent 执行一次受控决策。 */
@Service
public class TravelPlannerAgentService {

    private final PlannerContextService plannerContextService;
    private final PlannerAgentService plannerAgentService;
    private final ObjectMapper objectMapper;

    public TravelPlannerAgentService(
            PlannerContextService plannerContextService,
            PlannerAgentService plannerAgentService,
            ObjectMapper objectMapper) {
        this.plannerContextService = plannerContextService;
        this.plannerAgentService = plannerAgentService;
        this.objectMapper = objectMapper;
    }

    public PlannerAgentStepResult executeStep(
            Long userId,
            Long planId,
            String userRequest) {
        PlannerTravelContext context = plannerContextService.build(userId, planId);
        return plannerAgentService.executeNextStep(new PlannerAgentRequest(
                userRequest,
                serialize(context),
                null
        ));
    }

    private String serialize(PlannerTravelContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Planner旅行上下文序列化失败", exception);
        }
    }
}
