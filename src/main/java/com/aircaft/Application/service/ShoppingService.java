package com.aircaft.Application.service;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.pojo.dto.BuyDTO;
import com.aircaft.Application.pojo.vo.ShoppingVO;

import java.util.List;

public interface ShoppingService {
    Result<List<ShoppingVO>> getAllShopping();

    void buy(BuyDTO dto);
}
