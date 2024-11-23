package com.aircaft.Application.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BuyDTO {

    private Integer shoppingCentreId;
    private Integer propId;
    private Integer playerId;
    private Integer amount;
    private LocalDateTime buyTime;

}
