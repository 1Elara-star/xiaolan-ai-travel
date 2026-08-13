package com.lanyu.xiaolanaitravel.user;

import com.lanyu.xiaolanaitravel.user.dto.UserProfileRequest;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import com.lanyu.xiaolanaitravel.user.mapper.UserProfileMapper;
import com.lanyu.xiaolanaitravel.user.service.UserProfileService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceTests {

    @Test
    void partialUpdateKeepsFieldsThatWereNotSupplied() {
        UserProfileMapper mapper = mock(UserProfileMapper.class);
        UserProfile existing = new UserProfile();
        existing.setId(5L);
        existing.setUserId(7L);
        existing.setTravelPace("轻松");
        existing.setFoodPreference("本地小吃");
        when(mapper.selectOne(any())).thenReturn(existing);

        UserProfileRequest request = new UserProfileRequest();
        request.setTravelPace("适中");
        new UserProfileService(mapper).saveProfile(7L, request);

        assertEquals("适中", existing.getTravelPace());
        assertEquals("本地小吃", existing.getFoodPreference());
        verify(mapper).updateById(existing);
    }

    @Test
    void explicitBlankClearsAField() {
        UserProfileMapper mapper = mock(UserProfileMapper.class);
        UserProfile existing = new UserProfile();
        existing.setId(5L);
        existing.setUserId(7L);
        existing.setDislikeTags("早起");
        when(mapper.selectOne(any())).thenReturn(existing);

        UserProfileRequest request = new UserProfileRequest();
        request.setDislikeTags("   ");
        new UserProfileService(mapper).saveProfile(7L, request);

        assertNull(existing.getDislikeTags());
    }
}
