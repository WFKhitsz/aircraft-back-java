package com.aircaft.Application.pojo.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JoinGroupChat {
    private Integer groupId;
    private Integer playerId;
    private LocalDateTime joinTime;
}
