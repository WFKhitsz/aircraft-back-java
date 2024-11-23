package com.aircaft.Application.pojo.vo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginVo {
    private String token;
    private String playerName;
    private Integer playerId;
}
