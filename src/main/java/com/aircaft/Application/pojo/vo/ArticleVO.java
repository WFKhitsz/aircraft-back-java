package com.aircaft.Application.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleVO {
    String name;
    String description;
    LocalDateTime createDate;
    Integer type;
    Integer likeNum;
    Integer clickNum;
    Integer id;
}
