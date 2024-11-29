package com.aircaft.Application.service;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.ManageApply;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;

import java.util.List;

public interface UserService {
    Player login(UserLoginDTO dto);

    List<BackPackPropsVO> getPlayerBackpackProps();

    void useProp(Integer propId);

    void setAttributes(AircarftAttributesDTO dto);

    List<Player> getAllPlayer();

    void inertMessageToSingleChat(SinglePlayerChat chat);

    List<SinglePlayerChat> getPlayerChatMessage(String toPlayerName);

    void setFriend(String toPlayerName);

    List<GetFriendsVO> getFriends();

    void createGroup(String groupName);

    List<ChatGroup> getAllChatGroup();

    void joinChatGroup(Integer chatGroupId);


    List<ChatGroup> getPlayerChatGroup();

    List<Player> getPlayerChatGroupAnotherPlayers(Integer groupId);

    void insertGroupHistoryMessage(GroupMessage groupMessage);

    void saveGameInfo(Game game);

    List<SendGroupChatMessage> getChatGroupHistoryMessage(Integer groupId);

    void wantToJoinChatGroup(Integer chatGroupId);

    List<ChatGroupApply> getPlayerGroupApply();


    List<ChatGroupApply> getGroupApply();

    void manageGroupApply(ManageApply manageApply);

    List<MailMessage> getPlayerMailMessage();

    void changeMailMessage();
}
