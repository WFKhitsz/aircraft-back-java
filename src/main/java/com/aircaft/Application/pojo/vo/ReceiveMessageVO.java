package com.aircaft.Application.pojo.vo;

import lombok.Data;

@Data
public class ReceiveMessageVO {
    private Integer type;
    private String fromPlayerName;
    private String toPlayerName;
    private String message;

}
