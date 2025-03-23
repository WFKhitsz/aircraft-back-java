package com.aircaft.Application.service.impl;

import com.aircaft.Application.common.BaseContext;
import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.MessageConstant;
import com.aircaft.Application.common.exception.*;
import com.aircaft.Application.controller.user.PlayerWebSocket;
import com.aircaft.Application.mapper.UserMapper;
import com.aircaft.Application.pojo.dto.AircarftAttributesDTO;
import com.aircaft.Application.pojo.dto.ArticleChangeDTO;
import com.aircaft.Application.pojo.dto.ManageApply;
import com.aircaft.Application.pojo.dto.UserLoginDTO;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.ArticleVO;
import com.aircaft.Application.pojo.vo.BackPackPropsVO;
import com.aircaft.Application.pojo.vo.GetFriendsVO;
import com.aircaft.Application.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class UserServiceImpl implements UserService {
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
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
    @Transactional
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

    @Override
    public Result uploadMarkdownFile(MultipartFile file, String description, String type) throws IOException {
        //判断文章参数是否符合校验规则
        //校验一下type是否可以数字化
        try {
            Integer.parseInt(type);
        } catch (NumberFormatException e) {
            throw new ValidateException("格式错误");
        }
        if (description.length() > 100 || description.length() < 10) {
            throw new ValidateException("格式错误");
        }

        //还要判断文件名是否重复
        if (file.isEmpty()) {
            throw new FileIsEmpty("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".md")) {
            throw new FileTypeError("仅支持Markdown文件（.md）");
        }
        //检测文件冲突
        File targetPath = new File(UPLOAD_DIR, originalFilename);
        if (targetPath.exists()) {
            throw new FileConflict("文件已存在");
        }
        //准备插入数据库的数据

        Article article = new Article();
        article.setDescription(description);
        article.setName(originalFilename);
        article.setCreateDate(LocalDateTime.now());
        article.setType(Integer.parseInt(type));
        article.setLikeNum(0);
        article.setClickNum(0);
        // 3. 创建存储目录（如果不存在）
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        // 4. 保存文件到本地
        Path filePath = uploadPath.resolve(originalFilename);
        Files.copy(file.getInputStream(), filePath);
        userMapper.insertArticle(article);
        return Result.success(filePath);

    }

    @Override
    public ResponseEntity<Resource> getMarkdownFile(String fileName) {
        File file = Paths.get(UPLOAD_DIR, fileName).toFile();
        if (!file.exists()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            throw new FileNotExist("文件不存在");
        }
        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @Override
    public void FileConflictCheck(String fileName) {
        File targetPath = new File(UPLOAD_DIR, fileName);
        if (targetPath.exists()) {
            throw new FileConflict("文件已存在");
        }
    }

    @Override
    public Result uploadFileChunks(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks) throws IOException {
        // 创建用户专属的上传目录
        File userDir = new File(UPLOAD_DIR + fileId + "/");
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        // 将每个块单独存储，使用索引号命名
        File chunkFile = new File(userDir, "chunk_" + chunkIndex);
        chunk.transferTo(chunkFile);
        // 检查是否所有块都已上传完成
        if (Objects.requireNonNull(userDir.list()).length == totalChunks) {
            File finalFile = new File(UPLOAD_DIR, fileId.split("_")[1]);
            try (FileOutputStream fos = new FileOutputStream(finalFile, true)) {
                // 按顺序读取每个块并写入
                for (int i = 0; i < Objects.requireNonNull(userDir.list()).length; i++) {
                    File chunkFileNow = new File(userDir, "chunk_" + i);
                    byte[] chunkData = Files.readAllBytes(chunkFileNow.toPath());
                    fos.write(chunkData);
                }
            }
            // 清理临时目录
            for (File file : Objects.requireNonNull(userDir.listFiles())) {
                file.delete();
            }
            userDir.delete();
        }
        return Result.success("第" + (chunkIndex + 1) + "上传成功");
    }

    @Override
    public List<ArticleVO> getAllArticles() {
        return userMapper.getAllArticles();
    }

    @Override
    public void saveArticleChange(ArticleChangeDTO articleChangeDTO) {
        userMapper.saveArticleChange(articleChangeDTO);
    }

    @Override
    public List<ArticleVO> getFileByType(Integer type) {
        return userMapper.getFileByType(type);
    }

    @Override
    public List<ArticleVO> getFileLimit(Integer offset) {
        return userMapper.getFileLimit(offset * 10);
    }

    @Override
    public List<ArticleVO> getFilePopular() {
        return userMapper.getFilePopular();
    }

    @Override
    public Integer getArticlesClickNumb() {
        return userMapper.getArticlesClickNumb();
    }

    @Override
    public Integer getArticleNumByType(Integer type) {
        return userMapper.getArticleNumByType(type);
    }

    @Override
    public List<ArticleVO> searchArticles(String keys) {
        return userMapper.searchArticles(keys);
    }
}
