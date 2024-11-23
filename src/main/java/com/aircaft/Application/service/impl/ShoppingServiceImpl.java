package com.aircaft.Application.service.impl;

import com.aircaft.Application.common.BaseContext;
import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.MessageConstant;
import com.aircaft.Application.common.exception.PlayerMoneyNotEnough;
import com.aircaft.Application.mapper.ShoppingMapper;
import com.aircaft.Application.mapper.UserMapper;
import com.aircaft.Application.pojo.dto.BuyDTO;
import com.aircaft.Application.pojo.entity.Player;
import com.aircaft.Application.pojo.entity.ShoppingSpend;
import com.aircaft.Application.pojo.vo.ShoppingVO;
import com.aircaft.Application.service.ShoppingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingServiceImpl implements ShoppingService {
    @Autowired
    ShoppingMapper shoppingMapper;


    @Override
    public Result<List<ShoppingVO>> getAllShopping() {
        return Result.success(shoppingMapper.getAllShopping());
    }


    /**
     * 这里只支持单个类型的商品的购买，后续可以添加购物车功能
     */
    @Override
    @Transactional
    public void buy(BuyDTO dto) {
        List<ShoppingSpend> spendList = shoppingMapper.getAllShoppingSpend(dto.getShoppingCentreId());
        Integer cost = 0;
        for (ShoppingSpend shoppingSpend : spendList) {
            if (shoppingSpend.getPropId() == dto.getPropId()) {
                cost = shoppingSpend.getSpend() * dto.getAmount();
            }
        }
        Player player = shoppingMapper.getPlayer(BaseContext.getCurrentId());
        if (player.getMoney() < cost) {
            throw new PlayerMoneyNotEnough(MessageConstant.MONEY_NOT_ENOUGH);
        }
        shoppingMapper.costPlayerMoney(cost, BaseContext.getCurrentId());
        dto.setBuyTime(LocalDateTime.now());
        shoppingMapper.insertShoppingHistory(dto);
        shoppingMapper.addProp(dto);
    }
}
