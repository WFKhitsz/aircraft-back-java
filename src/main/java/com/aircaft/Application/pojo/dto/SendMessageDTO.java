package com.aircaft.Application.pojo.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    private Integer type;
    private String fromPlayerName;
    private String toPlayerName;
    private String message;
}
