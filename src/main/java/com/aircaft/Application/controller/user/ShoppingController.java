package com.aircaft.Application.controller.user;


import com.aircaft.Application.common.BaseContext;
import com.aircaft.Application.common.Result;
import com.aircaft.Application.mapper.ShoppingMapper;
import com.aircaft.Application.pojo.dto.BuyDTO;
import com.aircaft.Application.pojo.vo.ShoppingVO;
import com.aircaft.Application.service.ShoppingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shopping")
public class ShoppingController {
    @Autowired
    ShoppingService shoppingService;

    @GetMapping("/get")
    public Result<List<ShoppingVO>> get() {
        return shoppingService.getAllShopping();
    }

    @PostMapping("/buy")
    public Result buy(@RequestBody BuyDTO dto) {
        dto.setPlayerId(BaseContext.getCurrentId());
        shoppingService.buy(dto);
        return Result.success();
    }

}
