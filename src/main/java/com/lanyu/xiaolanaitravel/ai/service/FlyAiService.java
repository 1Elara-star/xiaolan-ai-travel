package com.lanyu.xiaolanaitravel.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FlyAiService {

    private final ObjectMapper objectMapper;

    public FlyAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FlyAiHotelResponse searchHotels(
            String destination,
            String poiName,
            Integer maxPrice) {

        List<String> command = new ArrayList<>();

        command.add("cmd");
        command.add("/c");
        command.add("flyai.cmd");
        command.add("search-hotel");

        command.add("--dest-name");
        command.add(destination);

        if (poiName != null && !poiName.isBlank()) {
            command.add("--poi-name");
            command.add(poiName);
        }

        if (maxPrice != null) {
            command.add("--max-price");
            command.add(String.valueOf(maxPrice));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            Process process = processBuilder.start();

            String json = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String error = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();

            if (json.isBlank()) {
                throw new RuntimeException(
                        "飞猪查询失败，exitCode=" + exitCode + "，error=" + error
                );
            }

            return objectMapper.readValue(
                    json.trim(),
                    FlyAiHotelResponse.class
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException("飞猪返回的酒店JSON解析失败", e);

        } catch (IOException e) {
            throw new RuntimeException("调用飞猪 FlyAI 失败", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("调用飞猪 FlyAI 被中断", e);
        }
    }
}