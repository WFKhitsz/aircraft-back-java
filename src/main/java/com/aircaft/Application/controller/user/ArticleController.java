package com.aircaft.Application.controller.user;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.pojo.entity.Article;
import com.aircaft.Application.pojo.vo.ArticleVO;
import com.aircaft.Application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    UserService userService;

    @GetMapping("/getFile/{fileName}")
    public ResponseEntity<Resource> getMarkdownFile(@PathVariable String fileName) {
        String name = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        return userService.getMarkdownFile(URLDecoder.decode(fileName, StandardCharsets.UTF_8));
    }

    @GetMapping("/getAllArticles")
    public Result<List<ArticleVO>> getAllArticles() {
        return Result.success(userService.getAllArticles());
    }

    @GetMapping("/getFileByType/{type}")
    public Result<List<ArticleVO>> getFileByType(@PathVariable(value = "type") Integer type) {
        return Result.success(userService.getFileByType(type));
    }

    @GetMapping("/getFileLimit/{offset}")
    public Result<List<ArticleVO>> getFileLimit(@PathVariable(value = "offset") Integer offset) {
        return Result.success(userService.getFileLimit(offset));
    }

    @GetMapping("/getFilePopular")
    public Result<List<ArticleVO>> getFilePopular() {
        return Result.success(userService.getFilePopular());
    }

    @GetMapping("/getArticleNumByType/{type}")
    public Result<Integer> getArticleNumByType(@PathVariable(value = "type") Integer type) {
        return Result.success(userService.getArticleNumByType(type));
    }


    @GetMapping("/searchArticles/{keyWords}")
    public Result<List<ArticleVO>> searchArticles(@PathVariable(value = "keyWords") String keyWords) {
        String keys = URLDecoder.decode(keyWords, StandardCharsets.UTF_8);
        return Result.success(userService.searchArticles(keys));

    }

}
