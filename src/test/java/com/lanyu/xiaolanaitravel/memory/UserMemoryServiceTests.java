package com.lanyu.xiaolanaitravel.memory;

import com.lanyu.xiaolanaitravel.memory.mapper.UserMemoryMapper;
import com.lanyu.xiaolanaitravel.memory.service.UserMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMemoryServiceTests {

    @Test
    void missingOwnedMemoryReturnsNotFound() {
        UserMemoryMapper mapper = mock(UserMemoryMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        UserMemoryService service = new UserMemoryService(mapper);

        assertThrows(ResponseStatusException.class, () -> service.get(7L, 12L));
        verify(mapper).selectOne(any());
    }
}
