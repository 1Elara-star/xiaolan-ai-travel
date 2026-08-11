package com.lanyu.xiaolanaitravel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户画像数据访问接口
 */
@Mapper
public interface UserProfileMapper
        extends BaseMapper<UserProfile> {
}