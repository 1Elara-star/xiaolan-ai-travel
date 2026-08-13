package com.lanyu.xiaolanaitravel.memory.controller;

import com.lanyu.xiaolanaitravel.memory.dto.UserMemoryRequest;
import com.lanyu.xiaolanaitravel.memory.dto.UserMemoryResponse;
import com.lanyu.xiaolanaitravel.memory.service.UserMemoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/memories")
public class UserMemoryController {

    private final UserMemoryService memoryService;

    public UserMemoryController(UserMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<UserMemoryResponse> list(
            @RequestAttribute("currentUserId") Long userId,
            @RequestParam(required = false)
            @Pattern(regexp = "PREFERENCE|DISLIKE|EXPERIENCE|REMINDER",
                    message = "记忆类型不支持") String memoryType,
            @RequestParam(required = false) Boolean confirmed) {
        return memoryService.list(userId, memoryType, confirmed);
    }

    @GetMapping("/{id}")
    public UserMemoryResponse get(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        return memoryService.get(userId, id);
    }

    @PostMapping
    public ResponseEntity<UserMemoryResponse> create(
            @RequestAttribute("currentUserId") Long userId,
            @Valid @RequestBody UserMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoryService.create(userId, request));
    }

    @PutMapping("/{id}")
    public UserMemoryResponse update(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UserMemoryRequest request) {
        return memoryService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestAttribute("currentUserId") Long userId,
            @PathVariable Long id) {
        memoryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
