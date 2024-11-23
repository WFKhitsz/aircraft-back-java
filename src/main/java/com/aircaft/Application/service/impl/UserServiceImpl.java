package com.aircaft.Application.service.impl;

import com.aircaft.Application.common.BaseContext;
import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.MessageConstant;
import com.aircaft.Application.common.exception.*;
import com.aircaft.Application.mapper.UserMapper;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import com.aircaft.Application.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public Player login(UserLoginDTO dto) {
        String username = dto.getUserName();
        String password = dto.getPassword();
        Player player = userMapper.getByName(username);
        if (player == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!password.equals(dto.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (player.getStatus() == 0) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return player;
    }

    @Override
    public List<BackPackPropsVO> getPlayerBackpackProps() {
        Integer id = BaseContext.getCurrentId();
        return userMapper.getPlayerBackpackProps(id);
    }

    @Override
    public void useProp(Integer propId) {
        List<BackPackPropsVO> playerBackpackProps = userMapper.getPlayerBackpackProps(BaseContext.getCurrentId());
        //先看看是不是有大于一个道具
        BackPackPropsVO prop = null;
        for (BackPackPropsVO playerBackpackProp : playerBackpackProps) {
            if (playerBackpackProp.getPropId() == propId) {
                prop = playerBackpackProp;
            }
        }
        if (prop != null) {
            if (prop.getAmount() > 1) {
                userMapper.updatePlayerBackpackPropAmount(BaseContext.getCurrentId(), propId);

            } else if (prop.getAmount() == 1) {
                userMapper.delPlayerBackpackProp(BaseContext.getCurrentId(), propId);

            }
        } else {
            throw new PropAmountNotEnough("玩家的道具数量不足");

        }

    }

    @Override
    public void setAttributes(AircarftAttributesDTO dto) {
        userMapper.setAttributes(dto);
    }

    @Override
    public List<Player> getAllPlayer() {
        List<Player> playerList = userMapper.getAllplayer();
        return playerList;
    }

    @Override
    public void inertMessageToSingleChat(SinglePlayerChat chat) {
        userMapper.inertMessageToSingleChat(chat);
    }

    @Override
    public List<SinglePlayerChat> getPlayerChatMessage(String toPlayerName) {
        Integer toId = userMapper.getIdByName(toPlayerName);
        List<SinglePlayerChat> list = userMapper.getPlayerChatMessage(BaseContext.getCurrentId(), toId);
        return list;
    }

    @Override
    public void setFriend(String toPlayerName) {
        Integer toId = userMapper.getIdByName(toPlayerName);
        userMapper.setFriend(BaseContext.getCurrentId(), toId, LocalDateTime.now());

    }

    @Override
    public List<GetFriendsVO> getFriends() {

        return userMapper.getFriends(BaseContext.getCurrentId());
    }

    @Override
    public void createGroup(String groupName) {
        //添补不可创建重复名字的群聊
        ChatGroup isExistSameNameGroup = userMapper.getGroupByName(groupName);
        if (isExistSameNameGroup != null) {
            throw new GroupNameAlreadyUsed("群聊的名字已经被使用了");
        }
        //没有被使用过，那就正常插入
        ChatGroup chatGroup = new ChatGroup();
        CreateChatGroup createChatGroup = new CreateChatGroup();
        chatGroup.setGroupName(groupName);
        //由于有触发器，会自动加1，所以这里设置为0
        chatGroup.setGroupPlayerAmount(0);
        chatGroup.setCreateTime(LocalDateTime.now());
        createChatGroup.setCreateTime(LocalDateTime.now());
        createChatGroup.setPlayerId(BaseContext.getCurrentId());
        createChatGroup.setGroupName(groupName);
        userMapper.createGroup(chatGroup);
        createChatGroup.setGroupId(chatGroup.getId());
        userMapper.createGroupHistory(createChatGroup);
        joinChatGroup(chatGroup.getId());

    }

    @Override
    public List<ChatGroup> getAllChatGroup() {

        return userMapper.getAllChatGroup();
    }

    @Override
    public void joinChatGroup(Integer chatGroupId) {
        //需要先判断一下是不是已经加入群聊
        final JoinGroupChat chatGroupPlayerByPlayerId = userMapper.getChatGroupPlayerByPlayerId(BaseContext.getCurrentId(), chatGroupId);
        if (chatGroupPlayerByPlayerId != null) {
            throw new AlreadyJoinChatGroupException(MessageConstant.PLAYER_ALREADY_JOIN_CHAT_GROUP);
        }
        userMapper.increaseChatGroupPlayerAmonut(chatGroupId);
        userMapper.joinChatGroup(chatGroupId, BaseContext.getCurrentId(), LocalDateTime.now());
    }

    @Override
    public List<ChatGroup> getPlayerChatGroup() {
        return userMapper.getPlayerChatGroup(BaseContext.getCurrentId());
    }

    @Override
    public List<Player> getPlayerChatGroupAnotherPlayers(Integer groupId) {
        return userMapper.getPlayerChatGroupAnotherPlayers(groupId);
    }

    @Override
    public void insertGroupHistoryMessage(GroupMessage groupMessage) {
        SendGroupChatMessage sendGroupChatMessage = new SendGroupChatMessage();
        Integer fromPlayerId = userMapper.getIdByName(groupMessage.getFromPlayerName());
        BeanUtils.copyProperties(groupMessage, sendGroupChatMessage);
        sendGroupChatMessage.setPlayerId(fromPlayerId);
        sendGroupChatMessage.setSendTime(LocalDateTime.now());
        userMapper.insertGroupHistoryMessage(sendGroupChatMessage);
    }

    @Override
    public void saveGameInfo(Game game) {
        userMapper.saveGameInfo(game);
    }

    @Override
    public List<SendGroupChatMessage> getChatGroupHistoryMessage(Integer groupId) {

        return userMapper.getChatGroupHistoryMessage(groupId, BaseContext.getCurrentId());
    }


}
