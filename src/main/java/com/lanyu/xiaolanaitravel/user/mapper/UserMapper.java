package com.lanyu.xiaolanaitravel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyu.xiaolanaitravel.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户数据访问接口，简单 CRUD 直接使用 MyBatis-Plus BaseMapper。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
