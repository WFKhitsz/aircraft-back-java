package com.aircaft.Application.pojo.entity;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Player {

    // 玩家主键
    private int playerId;

    // 玩家用户名
    private String userName;

    // 玩家密码
    private String password;

    // 创建时间
    private LocalDateTime createTime;

    // 玩家状态，0表示禁用，1表示启用
    private int status;

    // 玩家金钱
    private int money;
}
