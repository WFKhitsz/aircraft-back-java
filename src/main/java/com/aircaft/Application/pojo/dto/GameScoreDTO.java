package com.aircaft.Application.pojo.dto;


import lombok.Data;

@Data
public class GameScoreDTO {
    private Integer type;
    private Integer playerId;
    private Integer playerScore;
}
