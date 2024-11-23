package com.aircaft.Application.pojo.vo;

import com.aircaft.Application.pojo.entity.Game;
import lombok.Data;

@Data
public class ScoreMessageVo {
    private Integer type;
    private Game game;
}
