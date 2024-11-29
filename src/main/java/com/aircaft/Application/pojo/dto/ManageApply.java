package com.aircaft.Application.pojo.dto;


import lombok.Data;

@Data
public class ManageApply {
    //type = 0表示拒绝，type = 1 表示通过
    private Integer type;
    private Integer groupId;
    private String playerName;
    private Integer playerId;
}
