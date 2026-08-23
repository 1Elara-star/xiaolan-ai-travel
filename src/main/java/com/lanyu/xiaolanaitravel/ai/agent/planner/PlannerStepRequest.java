package com.lanyu.xiaolanaitravel.ai.agent.planner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 前端发起一次 Planner 决策时需要提供的内容。 */
public record PlannerStepRequest(
        @NotBlank(message = "当前旅行问题不能为空")
        @Size(max = 2000, message = "当前旅行问题不能超过2000个字符")
        String userRequest
) {
}
