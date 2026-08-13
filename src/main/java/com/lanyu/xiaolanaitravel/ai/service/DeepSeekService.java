package com.lanyu.xiaolanaitravel.ai.service;

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

    public DeepSeekService(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model) {

        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String chat(String message) {

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", """
                你叫小兰，是一个 AI 旅行规划助手。

                现在你的任务是根据用户提供的旅行需求，
                生成一份结构化旅行计划。

                必须只返回合法 JSON，不要返回 Markdown，
                不要使用 ```json 代码块，也不要在 JSON 前后添加解释文字。

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
                          "itemType": "ATTRACTION",
                          "description": "为什么这样安排"
                        }
                      ]
                    }
                  ]
                }

                itemType 只能使用：
                ATTRACTION、FOOD、HOTEL、EVENT、REST、OTHER。

                注意：
                1. 暂时不要编造精确距离和交通耗时。
                2. 暂时不要编造实时酒店价格、门票价格和营业时间。
                3. 用户没有提供的信息可以合理规划，但不要伪造实时事实。
                4.在没有外部酒店数据的情况下，不要自行生成具体酒店名称。
                                     如需要安排住宿，placeName 统一填写“待推荐酒店”，
                                          后续由真实酒店数据工具提供候选酒店。
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
}