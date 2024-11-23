package com.aircaft.Application.controller.user;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.JwtClaimsConstant;
import com.aircaft.Application.common.properties.JwtProperties;
import com.aircaft.Application.common.utils.JwtUtil;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.ChatGroup;
import com.aircaft.Application.pojo.entity.Player;
import com.aircaft.Application.pojo.entity.SendGroupChatMessage;
import com.aircaft.Application.pojo.entity.SinglePlayerChat;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import com.aircaft.Application.pojo.vo.UserLoginVo;
import com.aircaft.Application.service.UserService;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<UserLoginVo> login(@RequestBody UserLoginDTO dto) {
        Player player = userService.login(dto);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, player.getPlayerId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );
        UserLoginVo vo = UserLoginVo.builder()
                .playerId(player.getPlayerId())
                .playerName(player.getUserName())
                .token(token)
                .build();
        return Result.success(vo);
    }

    @GetMapping("/getbackpack")
    public Result<List<BackPackPropsVO>> getbackpack() {
        List<BackPackPropsVO> list = userService.getPlayerBackpackProps();
        return Result.success(list);
    }

    @PutMapping("/useprop")
    public Result useprop(Integer propId) {
        userService.useProp(propId);
        return Result.success();
    }

    @PutMapping("/setattributes")
    public Result setattributes(@RequestBody AircarftAttributesDTO dto) {
        userService.setAttributes(dto);
        return Result.success();
    }

    @GetMapping("/getPlayerChatMessage/{toPlayerName}")
    public Result<List<SinglePlayerChat>> getPlayerChatMessage(@PathVariable(value = "toPlayerName") String toPlayerName) {
        List<SinglePlayerChat> list = userService.getPlayerChatMessage(toPlayerName);
        return Result.success(list);
    }

    @GetMapping("/setFriend/{toPlayerName}")
    public Result setFriend(@PathVariable(value = "toPlayerName") String toPlayerName) {
        userService.setFriend(toPlayerName);
        return Result.success();
    }

    @GetMapping("/getFriends")
    public Result<List<GetFriendsVO>> getFriends() {
        return Result.success(userService.getFriends());
    }

    @PostMapping("/createGroup/{groupName}")
    public Result createGroup(@PathVariable(value = "groupName") String groupName) {
        userService.createGroup(groupName);
        return Result.success();
    }

    @GetMapping("/getChatGroup")
    public Result<List<ChatGroup>> getChatGroup() {
        return Result.success(userService.getAllChatGroup());
    }


    @PostMapping("/joinChatGroup/{chatGroupId}")
    public Result joinChatGroup(@PathVariable(value = "chatGroupId") Integer chatGroupId) {
        userService.joinChatGroup(chatGroupId);

        return Result.success();
    }

    @GetMapping("/getPlayerChatGroup")
    public Result<List<ChatGroup>> getPlayerChatGroup() {
        return Result.success(userService.getPlayerChatGroup());
    }

    @GetMapping("/getPlayerChatGroupAnotherPlayers/{groupId}")
    public Result<List<Player>> getPlayerChatGroupAnotherPlayers(@PathVariable(value = "groupId") Integer groupId) {
        return Result.success(userService.getPlayerChatGroupAnotherPlayers(groupId));
    }

    //获取指定聊天群组的所有消息
    @GetMapping("/getChatGroupHistoryMessage/{groupId}")
    public Result<List<SendGroupChatMessage>> getChatGroupHistoryMessage(@PathVariable(value = "groupId") Integer groupId) {
        List<SendGroupChatMessage> message = userService.getChatGroupHistoryMessage(groupId);
        return Result.success(message);
    }

}
