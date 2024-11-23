package com.aircaft.Application.mapper;


import com.aircaft.Application.pojo.dto.BuyDTO;
import com.aircaft.Application.pojo.entity.Player;
import com.aircaft.Application.pojo.entity.ShoppingSpend;
import com.aircaft.Application.pojo.vo.ShoppingVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingMapper {
    public List<ShoppingVO> getAllShopping();

    @Select("select prop_id,spend from aircraft.shopping_centre where centre_type = #{shoppingCentreId}")
    List<ShoppingSpend> getAllShoppingSpend(@Param("shoppingCentreId") Integer shoppingCentreId);

    @Update("update aircraft.player set money = money - #{cost} where player_id = #{currentId}")
    void costPlayerMoney(@Param("cost") Integer cost, @Param("currentId") Integer currentId);

    @Insert("insert into aircraft.buy_prop (player_id, shopping_centre_id, prop_id, buy_time, amount) VALUES (#{playerId}, #{shoppingCentreId},#{propId},#{buyTime},#{amount})")
    void insertShoppingHistory(BuyDTO dto);

    @Select("select * from aircraft.player where player_id = #{currentId}")
    Player getPlayer(@Param("currentId") Integer currentId);

    @Update("update aircraft.player_backpack set amount = amount + #{amount} where player_id = #{playerId} and prop_id = #{propId}")
    void addProp(BuyDTO dto);
}
