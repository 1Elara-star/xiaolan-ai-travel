package com.lanyu.xiaolanaitravel.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyu.xiaolanaitravel.memory.dto.UserMemoryRequest;
import com.lanyu.xiaolanaitravel.memory.dto.UserMemoryResponse;
import com.lanyu.xiaolanaitravel.memory.entity.UserMemory;
import com.lanyu.xiaolanaitravel.memory.mapper.UserMemoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserMemoryService {

    private final UserMemoryMapper memoryMapper;

    public UserMemoryService(UserMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    public List<UserMemoryResponse> list(Long userId, String memoryType, Boolean confirmed) {
        LambdaQueryWrapper<UserMemory> query = new LambdaQueryWrapper<>();
        query.eq(UserMemory::getUserId, userId)
                .eq(memoryType != null && !memoryType.isBlank(),
                        UserMemory::getMemoryType, memoryType)
                .eq(confirmed != null, UserMemory::getUserConfirmed, confirmed)
                .orderByDesc(UserMemory::getUpdateTime)
                .orderByDesc(UserMemory::getId);
        return memoryMapper.selectList(query).stream().map(this::toResponse).toList();
    }

    public UserMemoryResponse get(Long userId, Long memoryId) {
        return toResponse(getOwned(userId, memoryId));
    }

    public UserMemoryResponse create(Long userId, UserMemoryRequest request) {
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        apply(memory, request);
        memoryMapper.insert(memory);
        return toResponse(memoryMapper.selectById(memory.getId()));
    }

    public UserMemoryResponse update(Long userId, Long memoryId, UserMemoryRequest request) {
        UserMemory memory = getOwned(userId, memoryId);
        apply(memory, request);
        memoryMapper.updateById(memory);
        return toResponse(getOwned(userId, memoryId));
    }

    public void delete(Long userId, Long memoryId) {
        UserMemory memory = getOwned(userId, memoryId);
        memoryMapper.deleteById(memory.getId());
    }

    private UserMemory getOwned(Long userId, Long memoryId) {
        UserMemory memory = memoryMapper.selectOne(new LambdaQueryWrapper<UserMemory>()
                .eq(UserMemory::getId, memoryId)
                .eq(UserMemory::getUserId, userId));
        if (memory == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "旅行记忆不存在");
        }
        return memory;
    }

    private void apply(UserMemory memory, UserMemoryRequest request) {
        memory.setMemoryType(request.memoryType().strip());
        memory.setMemoryContent(request.memoryContent().strip());
        memory.setUserConfirmed(Boolean.TRUE.equals(request.userConfirmed()));
    }

    private UserMemoryResponse toResponse(UserMemory memory) {
        return new UserMemoryResponse(memory.getId(), memory.getMemoryType(),
                memory.getMemoryContent(), Boolean.TRUE.equals(memory.getUserConfirmed()),
                memory.getCreateTime(), memory.getUpdateTime());
    }
}
