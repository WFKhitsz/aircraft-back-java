package com.aircaft.Application.service.impl;

import com.aircaft.Application.common.BaseContext;
import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.MessageConstant;
import com.aircaft.Application.common.exception.*;
import com.aircaft.Application.controller.user.PlayerWebSocket;
import com.aircaft.Application.mapper.UserMapper;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.ManageApply;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import com.aircaft.Application.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public Player login(UserLoginDTO dto) {
        String username = dto.getUserName();
        String password = dto.getPassword();
        //如果一个用户已经在线的话，那么后面登录的用户就需要顶替他，使得只有一个用户在线
        if (PlayerWebSocket.isThisUserAlreadyLogin(username)) {
            PlayerWebSocket.logoutUser(username);
        }
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

    @Override
    public void wantToJoinChatGroup(Integer chatGroupId) {
        //需要检查玩家是不是已经加入了，避免重复加入
        final JoinGroupChat chatGroupPlayerByPlayerId = userMapper.getChatGroupPlayerByPlayerId(BaseContext.getCurrentId(), chatGroupId);
        if (chatGroupPlayerByPlayerId != null) {
            throw new AlreadyJoinChatGroupException(MessageConstant.PLAYER_ALREADY_JOIN_CHAT_GROUP);
        }
        //我们还需要检查是否重复申请
        ChatGroupApply chatGroupApply = userMapper.isPlayerAlreadyApply(BaseContext.getCurrentId(), chatGroupId);
        if (chatGroupApply != null) {
            throw new PlayerAlreadyApply("玩家已经申请过了，无需重复申请");
        }
        userMapper.wantToJoinChatGroup(BaseContext.getCurrentId(), chatGroupId);

    }

    @Override
    public List<ChatGroupApply> getPlayerGroupApply() {
        return userMapper.getPlayerGroupApply(BaseContext.getCurrentId());
    }

    @Override
    //获得自己管理的群聊申请
    public List<ChatGroupApply> getGroupApply() {
        List<ChatGroupApply> groupApply = userMapper.getGroupApply(BaseContext.getCurrentId());
        if (groupApply.get(0).getApplyGroupId() == null) {
            groupApply = null;
        }
        return groupApply;
    }

    @Override
    public void manageGroupApply(ManageApply manageApply) {
        String groupName = "";

        MailMessage mailMessage = new MailMessage();
        mailMessage.setFromPlayerId(BaseContext.getCurrentId());
        mailMessage.setIsRead(0);
        mailMessage.setSendTime(LocalDateTime.now());
        mailMessage.setPlayerId(manageApply.getPlayerId());

        manageApply.setPlayerId(userMapper.getIdByName(manageApply.getPlayerName()));
        final List<ChatGroupApply> groupApply = getGroupApply();
        boolean isYouHasRightToManageThisGroup = false;
        for (ChatGroupApply chatGroupApply : groupApply) {
            if (Objects.equals(chatGroupApply.getApplyGroupId(), manageApply.getGroupId())) {
                //申请修改的群聊在自己的管理群聊里面，其实这一步完全是为了安全考虑
                groupName = chatGroupApply.getGroupName();
                isYouHasRightToManageThisGroup = true;
                break;
            }
        }
        if (!isYouHasRightToManageThisGroup) {
            throw new NoRightToManageThisGroup("非法访问无权限的群聊");
        }
        if (manageApply.getType() == 0) {
            //拒绝,这里就应该涉及到玩家的收信箱了
            userMapper.delPlayerApply(manageApply);

            String message = "您申请加入的" + groupName + "群聊" + "已经被管理员拒绝";
            mailMessage.setMessage(message);
            userMapper.insertMailMessage(mailMessage);
            //发出通知到对应玩家的收信箱里
        } else if (manageApply.getType() == 1) {
            //同意
            userMapper.delPlayerApply(manageApply);
            //添加到群聊
            userMapper.increaseChatGroupPlayerAmonut(manageApply.getGroupId());
            userMapper.joinChatGroup(manageApply.getGroupId(), manageApply.getPlayerId(), LocalDateTime.now());
            //发出相应的通知
        } else {
            //非法参数
            throw new ParmError("非法参数");
        }
    }

    @Override
    public List<MailMessage> getPlayerMailMessage() {

        return userMapper.getPlayerMailMessage(BaseContext.getCurrentId());
    }

    @Override
    public void changeMailMessage() {
        userMapper.changeMailMessage(BaseContext.getCurrentId());
    }


}
