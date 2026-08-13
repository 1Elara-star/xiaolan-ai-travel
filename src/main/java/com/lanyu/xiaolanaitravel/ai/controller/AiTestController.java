package com.lanyu.xiaolanaitravel.ai.controller;

import com.lanyu.xiaolanaitravel.ai.service.DeepSeekService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiTestController {

    private final DeepSeekService deepSeekService;

    public AiTestController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/test")
    public String test(@RequestParam String message) {
        return deepSeekService.chat(message);
    }
}