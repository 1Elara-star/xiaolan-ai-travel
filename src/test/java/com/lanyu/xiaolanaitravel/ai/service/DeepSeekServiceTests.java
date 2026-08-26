package com.lanyu.xiaolanaitravel.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekServiceTests {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private DeepSeekService deepSeekService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(DeepSeekChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        deepSeekService = new DeepSeekService(chatClient, new ObjectMapper());
    }

    @Test
    void generateStructuredResponseUsesSpringAiAndParsesJson() {
        when(responseSpec.content()).thenReturn("{\"value\":\"ok\"}");

        SampleResponse response = deepSeekService.generateStructuredResponse(
                "只返回JSON",
                "测试请求",
                SampleResponse.class
        );

        assertEquals("ok", response.value());
        verify(requestSpec).system("只返回JSON");
        verify(requestSpec).user("测试请求");
        verify(requestSpec).options(any(DeepSeekChatOptions.class));
    }

    @Test
    void generateStructuredResponseRejectsInvalidJson() {
        when(responseSpec.content()).thenReturn("不是JSON");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                deepSeekService.generateStructuredResponse(
                        "只返回JSON",
                        "测试请求",
                        SampleResponse.class
                )
        );

        assertEquals("DeepSeek返回的结构化JSON解析失败", exception.getMessage());
    }

    @Test
    void chatRejectsBlankModelResponse() {
        when(responseSpec.content()).thenReturn(" ");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> deepSeekService.chat("测试请求")
        );

        assertEquals("DeepSeek返回内容为空", exception.getMessage());
    }

    private record SampleResponse(String value) {
    }
}
