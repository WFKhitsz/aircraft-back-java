package com.aircaft.Application.pojo.dto;


import lombok.Data;

@Data
public class ArticleChangeDTO {
    Integer id;
    String name;
    String description;
    Integer type;
}
