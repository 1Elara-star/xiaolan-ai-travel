package com.lanyu.xiaolanaitravel.ai.agent.planner;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlannerAgentControllerTests {

    @Test
    void shouldRunHttpStepWithAuthenticatedUserAndPathPlanId() throws Exception {
        TravelPlannerAgentService service = mock(TravelPlannerAgentService.class);
        PlannerWorkflowService workflowService = mock(PlannerWorkflowService.class);
        PlannerAgentStepResult expected = new PlannerAgentStepResult(
                PlannerToolName.NONE, "不需要外部工具", null, null);
        when(service.executeStep(7L, 12L, "帮我看看这趟旅行还缺什么"))
                .thenReturn(expected);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlannerAgentController(service, workflowService))
                .build();

        mockMvc.perform(post("/travel/plan/12/ai/planner/step")
                        .requestAttr("currentUserId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userRequest":"帮我看看这趟旅行还缺什么"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool").value("NONE"))
                .andExpect(jsonPath("$.reason").value("不需要外部工具"))
                .andExpect(jsonPath("$.toolResult").doesNotExist());

        verify(service).executeStep(7L, 12L, "帮我看看这趟旅行还缺什么");
    }

    @Test
    void shouldRunControlledWorkflowEndpoint() throws Exception {
        TravelPlannerAgentService service = mock(TravelPlannerAgentService.class);
        PlannerWorkflowService workflowService = mock(PlannerWorkflowService.class);
        PlannerWorkflowResponse expected = new PlannerWorkflowResponse(
                PlannerWorkflowStatus.COMPLETED,
                1,
                List.of(),
                new PlannerWorkflowFacts(List.of(), List.of(), List.of()),
                null
        );
        when(workflowService.run(7L, 12L, "查清楚晚上怎么回酒店"))
                .thenReturn(expected);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlannerAgentController(service, workflowService))
                .build();

        mockMvc.perform(post("/travel/plan/12/ai/planner/run")
                        .requestAttr("currentUserId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userRequest":"查清楚晚上怎么回酒店"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.toolCallCount").value(1));

        verify(workflowService).run(7L, 12L, "查清楚晚上怎么回酒店");
    }
}
