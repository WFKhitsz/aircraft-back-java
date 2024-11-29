package com.aircaft.Application.pojo.entity;


import lombok.Data;

@Data
public class ChatGroupApply {
    private Integer applyGroupId;
    private Integer applyPlayerId;
    private String groupName;
    private String userName;
}
