package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMessage {
    private Integer type;
    private String fromPlayerName;
    private String message;
    private Integer groupId;
}
