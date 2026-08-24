package com.lanyu.xiaolanaitravel.ai.agent.planner;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户针对自己的旅行计划执行一次 Planner 决策。 */
@RestController
@RequestMapping("/travel/plan/{planId}/ai/planner")
public class PlannerAgentController {

    private final TravelPlannerAgentService travelPlannerAgentService;
    private final PlannerWorkflowService plannerWorkflowService;

    public PlannerAgentController(
            TravelPlannerAgentService travelPlannerAgentService,
            PlannerWorkflowService plannerWorkflowService) {
        this.travelPlannerAgentService = travelPlannerAgentService;
        this.plannerWorkflowService = plannerWorkflowService;
    }

    @PostMapping("/step")
    public PlannerAgentStepResult executeStep(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody PlannerStepRequest request) {
        return travelPlannerAgentService.executeStep(
                userId, planId, request.userRequest());
    }

    /** 执行最多五次 Tool 调用的受控 Planner Workflow。 */
    @PostMapping("/run")
    public PlannerWorkflowResponse runWorkflow(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody PlannerStepRequest request) {
        return plannerWorkflowService.run(userId, planId, request.userRequest());
    }
}
