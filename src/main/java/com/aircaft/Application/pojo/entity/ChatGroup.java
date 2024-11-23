package com.aircaft.Application.pojo.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroup {
    private Integer id;
    private String groupName;
    private Integer groupPlayerAmount;
    private LocalDateTime createTime;
}
