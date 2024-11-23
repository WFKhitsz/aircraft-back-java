package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Game {
    private Integer player1Id;
    private Integer player2Id;
    private Integer player1Score;
    private Integer player2Score;
    private Integer gameDifficult;
    private Integer rank;
    private LocalDateTime gameTime;
}
