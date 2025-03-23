package com.aircaft.Application.mapper;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.ArticleChangeDTO;
import com.aircaft.Application.pojo.dto.ManageApply;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.ArticleVO;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("select * from aircraft.player where user_name = #{username}")
    Player getByName(@Param("username") String username);

    List<BackPackPropsVO> getPlayerBackpackProps(@Param("id") Integer id);

    @Update("update aircraft.player_backpack set amount = amount - 1 where player_id = #{playerId} and prop_id = #{propId}")
    void updatePlayerBackpackPropAmount(@Param("playerId") Integer playerId, @Param("propId") Integer propId);

    @Delete("delete from aircraft.player_backpack where player_id = #{playerId} and prop_id = #{propId}")
    void delPlayerBackpackProp(@Param("playerId") Integer playerId, @Param("propId") Integer propId);

    @Update("update aircraft.player_aircraft_attributes set player_blood = #{playerBlood} , player_bullet_rate = #{playerBulletRate} where player_id = #{playerId}")
    void setAttributes(AircarftAttributesDTO dto);

    @Select("select * from aircraft.player")
    List<Player> getAllplayer();

    @Insert("insert into aircraft.single_player_chat (player1_id, player2_id, message, send_time) values (#{player1Id},#{player2Id},#{message},#{sendTime})")
    void inertMessageToSingleChat(SinglePlayerChat chat);


    @Select("select player_id from aircraft.player where user_name = #{toPlayerName}")
    Integer getIdByName(String toPlayerName);

    @Select("select * from aircraft.single_player_chat where (player1_id = #{currentId}  and player2_id = #{toId}) or (player1_id = #{toId} and player2_id = #{currentId})")
    List<SinglePlayerChat> getPlayerChatMessage(Integer currentId, Integer toId);

    @Insert("insert into aircraft.friends (player1_id, player2_id, create_time) VALUES (#{currentId},#{toId},#{now})")
    void setFriend(@Param("currentId") Integer currentId, @Param("toId") Integer toId, @Param("now") LocalDateTime now);


    @Select("select player1.user_name as player1Name,player2.user_name as player2Name from aircraft.friends friends left join aircraft.player player1 on  friends.player1_id = player1.player_id   left join aircraft.player player2 on friends.player2_id = player2.player_id where player1_id = #{currentId }  or player2_id =  #{currentId }")
    List<GetFriendsVO> getFriends(Integer currentId);


    @Insert("insert into aircraft.chat_group (group_name, group_player_amount, create_time) VALUES (#{groupName},#{groupPlayerAmount},#{createTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void createGroup(ChatGroup chatGroup);

    @Insert("insert into aircraft.create_group_chat (group_id, player_id, create_time, group_name) VALUES (#{groupId},#{playerId},#{createTime},#{groupName})")
    void createGroupHistory(CreateChatGroup createChatGroup);

    @Select("select * from aircraft.chat_group ")
    List<ChatGroup> getAllChatGroup();

    @Insert("INSERT INTO aircraft.join_group_chat (group_id, player_id, join_time) VALUES (#{chatGroupId},#{currentId},#{now})")
    void joinChatGroup(@Param("chatGroupId") Integer chatGroupId, @Param("currentId") Integer currentId, @Param("now") LocalDateTime now);

    @Update("update aircraft.chat_group set group_player_amount = group_player_amount +1 where id = #{chatGroupId}")
    void increaseChatGroupPlayerAmonut(@Param("chatGroupId") Integer chatGroupId);

    @Select("select * from  aircraft.join_group_chat where group_id = #{groupId} and player_id = #{currentId}")
    JoinGroupChat getChatGroupPlayerByPlayerId(@Param("currentId") Integer currentId, @Param("groupId") Integer groupId);

    @Select("select chat_group.id,chat_group.group_player_amount,chat_group.group_name,chat_group.create_time from aircraft.join_group_chat left join aircraft.chat_group on join_group_chat.group_id = chat_group.id where player_id = #{currentId}")
    List<ChatGroup> getPlayerChatGroup(@Param("currentId") Integer currentId);

    @Select("select player.player_id,player.user_name from aircraft.join_group_chat left join aircraft.player on join_group_chat.player_id = player.player_id where join_group_chat.group_id = #{groupId}")
    List<Player> getPlayerChatGroupAnotherPlayers(Integer groupId);


    @Insert("insert into aircraft.send_group_chat_message (group_id, player_id, message, send_time) VALUES (#{groupId},#{playerId},#{message},#{sendTime})")
    void insertGroupHistoryMessage(SendGroupChatMessage sendGroupChatMessage);

    @Insert("insert into aircraft.multi_player (player1_id, game_difficulty_id, rank_id, player2_id, game_time, player1_score, player2_score) VALUES (#{player1Id},#{gameDifficult},#{rank},#{player2Id},#{gameTime},#{player1Score},#{player2Score})")
    void saveGameInfo(Game game);

    @Select("select  * from aircraft.send_group_chat_message where  group_id = #{groupId}")
    List<SendGroupChatMessage> getChatGroupHistoryMessage(@Param("groupId") Integer groupId, @Param("playerId") Integer currentId);


    @Select("select * from aircraft.chat_group where group_name = #{groupName}")
    ChatGroup getGroupByName(@Param("groupName") String groupName);

    @Insert("insert into aircraft.apply_for_join_chat_group (apply_group_id, apply_player_id) VALUES (#{chatGroupId},#{currentId})")
    void wantToJoinChatGroup(@Param("currentId") Integer currentId, @Param("chatGroupId") Integer chatGroupId);

    @Select("select * from aircraft.apply_for_join_chat_group where apply_group_id = #{chatGroupId}  and apply_player_id = #{currentId}")
    ChatGroupApply isPlayerAlreadyApply(@Param("currentId") Integer currentId, @Param("chatGroupId") Integer chatGroupId);

    @Select("select apply_group_id,apply_player_id,group_name from aircraft.apply_for_join_chat_group left join aircraft.chat_group on apply_for_join_chat_group.apply_group_id = chat_group.id   where apply_player_id = #{currentId};")
    List<ChatGroupApply> getPlayerGroupApply(Integer currentId);

    @Select("select apply_for_join_chat_group.apply_group_id,apply_for_join_chat_group.apply_player_id ,group_name ,player.user_name from aircraft.create_group_chat left join aircraft.apply_for_join_chat_group on create_group_chat.group_id = apply_for_join_chat_group.apply_group_id left join aircraft.player on apply_player_id = player.player_id where create_group_chat.player_id = #{currentId}")
    List<ChatGroupApply> getGroupApply(@Param("currentId") Integer currentId);

    @Delete("delete from aircraft.apply_for_join_chat_group where apply_player_id = #{playerId} and apply_group_id = #{groupId}")
    void delPlayerApply(ManageApply manageApply);

    @Insert("insert into aircraft.player_mail (player_id, message, from_player_id, send_time, is_read) VALUES (#{playerId},#{message},#{fromPlayerId},#{sendTime},#{isRead})")
    void insertMailMessage(MailMessage mailMessage);

    @Select("select * from aircraft.player_mail where player_id = #{currentId} ")
    List<MailMessage> getPlayerMailMessage(@Param("currentId") Integer currentId);

    @Update("update aircraft.player_mail set is_read = 1 where player_id  = #{currentId}")
    void changeMailMessage(@Param("currentId") Integer currentId);

    @Insert("insert into aircraft.article (name, description, click_num, create_date, type, like_num) VALUES (#{name},#{description},#{clickNum},#{createDate},#{type},#{likeNum})")
    void insertArticle(Article article);

    @Select("select id, name, description, click_num, create_date, type, like_num from aircraft.article order by id DESC ")
    List<ArticleVO> getAllArticles();

    @Update("update aircraft.article set name = #{name}, description = #{description},type = #{type} where id = #{id}")
    void saveArticleChange(ArticleChangeDTO articleChangeDTO);

    @Select("select * from aircraft.article where type = #{type} ")
    List<ArticleVO> getFileByType(@Param("type") Integer type);

    @Select("select * from aircraft.article LIMIT 10 offset #{offset}")
    List<ArticleVO> getFileLimit(@Param("offset") int offset);

    @Select("select * from aircraft.article ORDER BY click_num DESC LIMIT 10")
    List<ArticleVO> getFilePopular();

    @Select("SELECT SUM(click_num) from aircraft.article ")
    Integer getArticlesClickNumb();

    @Select("select count(*) from aircraft.article where type = #{type}")
    Integer getArticleNumByType(@Param("type") Integer type);

    @Select("select * from aircraft.article where description like  concat('%',#{keys},'%')")
    List<ArticleVO> searchArticles(@Param("keys") String keys);
}
