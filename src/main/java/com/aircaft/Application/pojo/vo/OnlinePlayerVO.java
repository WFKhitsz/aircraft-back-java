package com.aircaft.Application.pojo.vo;

import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class OnlinePlayerVO {
    private Integer type;
    private List<String> onLinePlayerName;
    private AtomicInteger onLinePlayerAmount;
}
