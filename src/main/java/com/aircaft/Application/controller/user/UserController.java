package com.aircaft.Application.controller.user;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.JwtClaimsConstant;
import com.aircaft.Application.common.properties.JwtProperties;
import com.aircaft.Application.common.utils.JwtUtil;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.ArticleChangeDTO;
import com.aircaft.Application.pojo.dto.ManageApply;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import com.aircaft.Application.pojo.vo.UserLoginVo;
import com.aircaft.Application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    //TODO 下面的joinChatGroup应该废弃 这里的加入群聊的功能还是太简单了，应该向群聊的管理员发出加入申请，等管理员批准后才能加入
    @PostMapping("/joinChatGroup/{chatGroupId}")
    public Result joinChatGroup(@PathVariable(value = "chatGroupId") Integer chatGroupId) {
        userService.joinChatGroup(chatGroupId);
        return Result.success();
    }

    //下面是申请操作，对应的就是原来的join操做
    @PostMapping("/wantToJoinChatGroup/{chatGroupId}")
    public Result wantToJoinChatGroup(@PathVariable(value = "chatGroupId") Integer chatGroupId) {
        userService.wantToJoinChatGroup(chatGroupId);
        return Result.success();
    }

    //玩家可以管理自己的群聊的申请，是不是应该给玩家配置一个收信箱，让他们知道有一些相关的消息，
    @GetMapping("/getPlayerGroupApply")
    public Result<List<ChatGroupApply>> getPlayerGroupApply() {
        List<ChatGroupApply> list = userService.getPlayerGroupApply();
        return Result.success(list);
    }

    //群管理员管理群聊的加入申请,这里我们希望可以得到申请玩家的名字
    @GetMapping("/getGroupApply")
    public Result<List<ChatGroupApply>> getGroupApply() {
        return Result.success(userService.getGroupApply());
    }

    //群管理员可以同意或拒绝,为了安全应该校验发送请求方到底有没有权限
    @PostMapping("/manageGroupApply")
    public Result manageGroupApply(@RequestBody ManageApply manageApply) {
        userService.manageGroupApply(manageApply);
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


    //获取自己的邮箱信息
    @GetMapping("/getPlayerMailMessage")
    public Result<List<MailMessage>> getPlayerMailMessage() {
        return Result.success(userService.getPlayerMailMessage());
    }

    //修改邮箱的消息状态为已读,玩家打开邮箱的时候触发
    public Result changeMailMessage() {
        userService.changeMailMessage();
        return Result.success();
    }

    //处理上传文章
    @PostMapping("/uploadFile")
    public Result uploadMarkdownFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam("type") String type
    ) throws IOException {
        return userService.uploadMarkdownFile(file, description, type);
    }

    @PostMapping("uploadFileChunks")
    public Result uploadFileChunks(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("fileId") String fileId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks
    ) throws IOException {
        return userService.uploadFileChunks(chunk, fileId, chunkIndex, totalChunks);
    }

    @GetMapping("/getFile/{fileName}")
    public ResponseEntity<Resource> getMarkdownFile(@PathVariable String fileName) {
        return userService.getMarkdownFile(fileName);
    }

    @PostMapping("/saveArticleChange")
    public Result saveArticleChange(@RequestBody ArticleChangeDTO articleChangeDTO) {
        userService.saveArticleChange(articleChangeDTO);
        return Result.success();
    }

    @GetMapping("/getArticlesClickNum")
    public Result<Integer> getArticlesClickNum() {
        return Result.success(userService.getArticlesClickNumb());
    }


}
