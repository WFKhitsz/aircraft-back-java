package com.aircaft.Application.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {
    String name;
    String description;
    LocalDateTime createDate;
    Integer type;
    Integer likeNum;
    Integer clickNum;

}
