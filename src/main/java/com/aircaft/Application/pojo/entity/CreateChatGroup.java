package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateChatGroup {
    private Integer groupId;
    private Integer playerId;
    private String groupName;
    private LocalDateTime createTime;
}
