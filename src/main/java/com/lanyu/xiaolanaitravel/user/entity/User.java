package com.lanyu.xiaolanaitravel.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户实体，与现有 MySQL user 表保持一致。 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号。 */
    private String username;

    /** 密码字段禁止通过接口返回；后续注册功能应在入库前使用 BCrypt。 */
    @JsonIgnore
    private String password;

    private String nickname;
    private String avatar;
    private String phone;
    private String email;

    /** USER 为普通用户，ADMIN 为管理员。 */
    private String role;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
