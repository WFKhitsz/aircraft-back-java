package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MailMessage {
    private Integer playerId;
    private String message;
    private Integer fromPlayerId;
    private LocalDateTime sendTime;
    private Integer isRead;
}
