package com.aircaft.Application.pojo.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SinglePlayerChat {
    private Integer player1Id;
    private Integer player2Id;
    private String message;
    private LocalDateTime sendTime;
}
