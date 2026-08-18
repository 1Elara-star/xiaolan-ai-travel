package com.lanyu.xiaolanaitravel.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.dto.AiTravelPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public DeepSeekService(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model,
            ObjectMapper objectMapper) {

        this.model = model;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 调用 DeepSeek 生成结构化旅行计划 JSON。
     */
    public String chat(String message) {

        Map<String, Object> requestBody = Map.of(
                "model", model,

                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                                你叫小兰，是一个 AI 旅行规划助手。

                                你的任务是根据用户已经确认的旅行需求，
                                生成一份结构化、合理、可执行的旅行计划。

                                必须只返回合法 JSON。
                                不要返回 Markdown。
                                不要使用 ```json 代码块。
                                不要在 JSON 前后添加任何解释文字。

                                JSON 格式必须如下：

                                {
                                  "destination": "目的地",
                                  "travelDays": 3,
                                  "summary": "行程整体说明",
                                  "days": [
                                    {
                                      "dayNumber": 1,
                                      "theme": "当天主题",
                                      "items": [
                                        {
                                          "placeName": "地点名称",
                                          "startTime": "09:00",
                                          "endTime": "11:00",
                                          "endDayOffset": 0,
                                          "itemType": "ATTRACTION",
                                          "description": "为什么这样安排"
                                        }
                                      ]
                                    }
                                  ]
                                }

                                itemType 只能使用：

                                ATTRACTION
                                FOOD
                                HOTEL
                                EVENT
                                REST
                                OTHER

                                【事实数据规则】

                                1. 不要编造精确交通距离和交通耗时。

                                2. 不要编造实时酒店价格、实时门票价格和实时营业时间。

                                3. 没有真实酒店数据时，
                                   不得自行生成具体酒店名称。

                                   如果行程需要住宿，
                                   HOTEL 节点的 placeName 统一填写：
                                   “待推荐酒店”。

                                   后续将由真实酒店数据工具提供候选酒店。

                                4. 地点地址、经纬度、实际路线等信息，
                                   后续由地图工具提供，
                                   当前不要自行猜测。

                                【时间规则——必须严格遵守】

                                1. startTime 和 endTime 使用 HH:mm 格式，
                                   例如：
                                   09:00
                                   11:30
                                   21:45

                                2. 时间范围只能是 00:00 到 23:59，
                                   不允许使用 24:00。

                                3. 每个节点必须返回 endDayOffset：
                                   0 表示结束时间仍在 dayNumber 当天；
                                   1 表示结束时间在次日。

                                4. 当 endDayOffset 为 0，且 startTime、endTime 都不为空时，
                                   endTime 必须严格晚于 startTime。

                                5. 夜班交通、跨午夜活动可以保留为一个完整节点，
                                   此时 endDayOffset 必须为 1。

                                当天节点示例：
                                startTime = "09:00"
                                endTime = "11:00"
                                endDayOffset = 0

                                跨天节点示例：
                                startTime = "23:30"
                                endTime = "06:30"
                                endDayOffset = 1

                                6. 不要为了绕过跨天校验而把真实存在的结束时间改成 null。
                                   只有确实无法合理确定时间时，才可以返回 null；
                                   非跨天节点即使时间为空，endDayOffset 仍填写 0。

                                7. HOTEL 节点只表示办理入住、返回酒店等行程动作，
                                   不需要用一个 HOTEL 节点表示整晚睡眠。

                                【行程规划规则】

                                1. 必须按照用户要求的旅行天数生成完整行程。

                                2. dayNumber 从 1 开始连续排列。

                                3. 每天安排合理数量的节点，
                                   避免为了丰富内容而安排过多地点。

                                4. 应结合用户预算、同行情况、旅行类型、
                                   旅行节奏和旅行偏好进行规划。

                                5. 用户明确提出“不想太累”“行程轻松”等要求时，
                                   应减少每天地点数量并预留休息时间。

                                6. 不要因为缺少实时信息而自行虚构事实。
                                """
                        ),

                        Map.of(
                                "role", "user",
                                "content", message
                        )
                ),

                "response_format", Map.of(
                        "type", "json_object"
                ),

                "thinking", Map.of(
                        "type", "disabled"
                ),

                "stream", false
        );

        Map response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

        Map<String, Object> messageObject =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) messageObject.get("content");
    }

    /**
     * 调用 DeepSeek，
     * 并把返回的 JSON 转换成 Java DTO。
     */
    public AiTravelPlanResponse generateTravelPlan(String message) {

        String json = chat(message);

        try {
            return objectMapper.readValue(
                    json,
                    AiTravelPlanResponse.class
            );

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "DeepSeek返回的行程JSON解析失败",
                    e
            );
        }
    }
}
