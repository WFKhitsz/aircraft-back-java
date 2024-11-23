package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendGroupChatMessage {
    private Integer groupId;
    private Integer playerId;
    private String message;
    private LocalDateTime sendTime;
}
