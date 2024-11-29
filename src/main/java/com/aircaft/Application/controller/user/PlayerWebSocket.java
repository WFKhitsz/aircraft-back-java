package com.aircaft.Application.controller.user;

import com.aircaft.Application.common.Result;
import com.aircaft.Application.common.constant.JwtClaimsConstant;
import com.aircaft.Application.common.constant.WebSocketConstant;
import com.aircaft.Application.common.properties.JwtProperties;
import com.aircaft.Application.common.utils.JsonUtil;
import com.aircaft.Application.common.utils.JwtUtil;
import com.aircaft.Application.pojo.dto.*;
import com.aircaft.Application.pojo.entity.*;
import com.aircaft.Application.pojo.vo.*;
import com.aircaft.Application.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint(value = "/ws/player")
@Slf4j
@Component
public class PlayerWebSocket {
    private static final ScheduledExecutorService onlinePlayMatch = Executors.newScheduledThreadPool(2);
    private static final ScheduledExecutorService sendGameInfoToMatchPlayer = Executors.newScheduledThreadPool(2);
    public static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static Map<String, Integer> playerNameToId = new ConcurrentHashMap<>();
    public static List<String> onLinePlayerName = new CopyOnWriteArrayList<>();
    public static AtomicInteger onLinePlayerAmount = new AtomicInteger(0);
    //难度为easy的匹配队列
    public static List<Integer> easyOnlinePlayPlayerReadyMatch = new CopyOnWriteArrayList<>();
    //难度为normal的匹配队列
    public static List<Integer> normalOnlinePlayPlayerReadyMatch = new CopyOnWriteArrayList<>();
    //难度为difficulty的匹配队列
    public static List<Integer> difficultyOnlinePlayPlayerReadyMatch = new CopyOnWriteArrayList<>();

    //    public static List<Integer> onlinePlayPlayerReadyMatch = new CopyOnWriteArrayList<>();
    public static Map<Integer, Integer> onlinePlayPlayerBeginMatch = new ConcurrentHashMap<>();
    //玩家对应的具体游戏
    public static Map<Integer, Game> playerToGame = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> onlinePlayerState = new ConcurrentHashMap<>();
    private static JwtProperties jwtProperties;
    private static UserService userService;
    private static List<Player> playerList;

    //玩家的匹配的游戏应该可以选择不同的游戏难度，那么就应该有多个不同的匹配队列
    static {
        onlinePlayMatch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //easy难度的游戏匹配逻辑
//                matchPlayerFromDifferentGameType(onlinePlayPlayerReadyMatch, 1);
                matchPlayerFromDifferentGameType(easyOnlinePlayPlayerReadyMatch, 1);
                matchPlayerFromDifferentGameType(normalOnlinePlayPlayerReadyMatch, 2);
                matchPlayerFromDifferentGameType(difficultyOnlinePlayPlayerReadyMatch, 3);

            }
        }, 1, 1, TimeUnit.SECONDS);


        sendGameInfoToMatchPlayer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
//                log.info("playerToGame:{}", playerToGame);
                if (playerToGame.size() < 1) {
                    return;
                }
                try {
                    for (Map.Entry<Integer, Game> gameEntry : playerToGame.entrySet()) {
                        final Session session1 = idToSession(gameEntry.getKey());
                        Game game = gameEntry.getValue();
                        ScoreMessageVo messageVo = new ScoreMessageVo();
                        messageVo.setGame(game);
                        messageVo.setType(WebSocketConstant.ONLINE_PLAY_ON);
                        synchronized (session1) {
                            session1.getAsyncRemote().sendText(JsonUtil.toJson(messageVo));
                        }

                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    throw new RuntimeException(e);
                }

            }
        }, 1, 500, TimeUnit.MILLISECONDS);
    }

    private Integer playerId;
    private String playerName;
    private Session session;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture future;
    private boolean heartBeat = false;
    private CompletableFuture<Void> pongReceived = new CompletableFuture<>();

    private static void matchPlayerFromDifferentGameType(List<Integer> readyMatch, Integer gameDiffculty) {

        if (readyMatch.size() <= 1) {
            return;
        }
        String player1Name = null;
        String player2Name = null;

        Integer player1Id = readyMatch.get(0);
        readyMatch.remove(0);
        Integer player2Id = readyMatch.get(0);
        readyMatch.remove(0);
        onlinePlayPlayerBeginMatch.put(player1Id, player2Id);
        log.info("{} type 游戏匹配成功：player1Id：{} player2Id：{}", gameDiffculty, player1Id, player2Id);
        //创建游戏并且设置它的初始值
        Game game = new Game();
        game.setPlayer1Id(player1Id);
        game.setPlayer2Id(player2Id);
        game.setPlayer1Score(0);
        game.setPlayer2Score(0);
        game.setGameTime(LocalDateTime.now());
        //这里的排行榜和难度一样
        game.setRank(gameDiffculty);

        //这里设置我们传递过来的难度参数
        game.setGameDifficult(gameDiffculty);
        //游戏才刚开始不设置游戏分数，应该在游戏结束后才设置
        //把游戏和玩家关联起来
        playerToGame.put(player1Id, game);
        playerToGame.put(player2Id, game);
        onlinePlayerState.put(player1Id, WebSocketConstant.ONLINE_GAME_STATE_START);
        onlinePlayerState.put(player2Id, WebSocketConstant.ONLINE_GAME_STATE_START);
        //找到玩家的名字
        for (Player player : playerList) {
            if (player.getPlayerId() == player1Id) {
                player1Name = player.getUserName();
            } else if (player.getPlayerId() == player2Id) {
                player2Name = player.getUserName();
            }
        }
        //获取到玩家对应的session
        Session player1Session = sessionMap.get(player1Name);
        Session player2Session = sessionMap.get(player2Name);
        //设置开始游戏信号
        OnlinePlayStart toPlayer1 = new OnlinePlayStart();
        OnlinePlayStart toPlayer2 = new OnlinePlayStart();
        toPlayer1.setType(WebSocketConstant.ONLINE_PLAY_START);
        toPlayer1.setAnotherPlayerId(player2Id);
        toPlayer1.setAnotherPlayerName(player2Name);
        toPlayer2.setType(WebSocketConstant.ONLINE_PLAY_START);
        toPlayer2.setAnotherPlayerId(player1Id);
        toPlayer2.setAnotherPlayerName(player1Name);
        try {
            synchronized (player1Session) {
                player1Session.getBasicRemote().sendText(JsonUtil.toJson(toPlayer1));
            }
            synchronized (player2Session) {
                player2Session.getBasicRemote().sendText(JsonUtil.toJson(toPlayer2));
            }
        } catch (Exception e) {
            log.info("onlinePlayMatch err:{}", e.getMessage());
            e.printStackTrace();
        }

    }

    //提供一个静态的接口方法，用于查询一个用户是否已经登录
    public static boolean isThisUserAlreadyLogin(String userName) {
        //我们只需要去sessionmap里面查一下就好
        return sessionMap.containsKey(userName);
    }

    //提供一个强制下线用户的方法
    public static void logoutUser(String userName) {
        try {
            LogoutMessage message = new LogoutMessage();
            message.setType(WebSocketConstant.USER_NEED_TO_LOGOUT);
            Session session1 = sessionMap.get(userName);
            synchronized (session1) {
                session1.getBasicRemote().sendText(JsonUtil.toJson(message));
                session1.close();
            }


        } catch (Exception e) {
            log.info("error : {}", e.getMessage());
        }
    }

    private static Session idToSession(Integer playerId) {
        String playerName = null;
        for (Map.Entry<String, Integer> entry : playerNameToId.entrySet()) {
            if (Objects.equals(entry.getValue(), playerId)) {
                playerName = entry.getKey();
            }
        }
        return sessionMap.get(playerName);
    }

    public static void sendMessageFromPlayerToPlayer(SendMessageDTO dto) throws Exception {
        Session toPlayerSession = sessionMap.get(dto.getToPlayerName());
        ReceiveMessageVO receiveMessageVO = new ReceiveMessageVO();
        BeanUtils.copyProperties(dto, receiveMessageVO);
        receiveMessageVO.setType(WebSocketConstant.SEND_MESSAGE_FROM_PLAYER_TO_PLAYER);
        //给对应的session加锁，防止多线程问题
        synchronized (toPlayerSession) {
            toPlayerSession.getAsyncRemote().sendText(JsonUtil.toJson(receiveMessageVO));
        }


        SinglePlayerChat chat = new SinglePlayerChat();
        chat.setPlayer1Id(PlayerWebSocket.playerNameToId.get(dto.getFromPlayerName()));
        chat.setPlayer2Id(PlayerWebSocket.playerNameToId.get(dto.getToPlayerName()));
        chat.setMessage(dto.getMessage());
        chat.setSendTime(LocalDateTime.now());
        userService.inertMessageToSingleChat(chat);
    }

    public void sendAllPlayerOnlinePlayer() {
        //由于session不是线程安全的，所以必须加锁，
        try {
            for (String key : sessionMap.keySet()) {
                sendOnlinePlayers(sessionMap.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    public void setJwtProperties(JwtProperties jwtProperties) {
        PlayerWebSocket.jwtProperties = jwtProperties;
    }

    @Autowired
    public void setUserService(UserService userService) {
        PlayerWebSocket.userService = userService;
        PlayerWebSocket.playerList = userService.getAllPlayer();
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            this.session = session;
            String token = session.getQueryString().split("&")[1].split("=")[1];
            String playerName = session.getQueryString().split("&")[2].split("=")[1];
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Integer userId = Integer.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            this.heartBeat = true;
            // 每隔 10 秒发送 ping 帧，并等待 pong
            session.addMessageHandler(PongMessage.class, message -> {
                pongReceived.complete(null);
            });
            future = executorService.scheduleAtFixedRate(() -> sendPingAndCheckPong(session), 1, 3, TimeUnit.SECONDS);
            log.info("webSocket连接当前用户id：{}", userId);
            this.playerId = userId;
            this.playerName = playerName;
            playerNameToId.put(playerName, userId);
            onLinePlayerName.add(this.playerName);
            onLinePlayerAmount.incrementAndGet();
            sessionMap.put(playerName, session);
        } catch (Exception e) {
            log.error("e: {}", e.getMessage());
            try {
                this.session.getBasicRemote().sendText(JsonUtil.toJson(Result.error("验证错误")));
                this.session.close();
            } catch (Exception exception) {
                log.error("e: {}", exception.getMessage());
            }
        }
    }

    private void sendOnlinePlayers(Session session) throws Exception {
        OnlinePlayerVO onlinePlayerVO = new OnlinePlayerVO();
        onlinePlayerVO.setOnLinePlayerAmount(onLinePlayerAmount);
        onlinePlayerVO.setOnLinePlayerName(onLinePlayerName);
        onlinePlayerVO.setType(WebSocketConstant.SEND_ONLINE_PLAYER_MESSAGE);
        synchronized (session) {
            session.getBasicRemote().sendText(JsonUtil.toJson(onlinePlayerVO));
        }

    }

    @OnClose
    public void onClose(CloseReason closeReason) {
        //验证失败什么都不要做
        if (this.playerName == null) {
            return;
        }
        // 关闭定时任务
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // 3. 如果 pongReceived CompletableFuture 尚未完成，可以选择取消或完成
        if (pongReceived != null && !pongReceived.isDone()) {
            pongReceived.complete(null);  // 或者调用 pongReceived.cancel(true);
        }
        onLinePlayerAmount.decrementAndGet();
        onLinePlayerName.remove(this.playerName);
//        playerNameToId.remove(this.playerName);
        sessionMap.remove(this.playerName);
        log.info("sesionList: {}", sessionMap);
        //群发最新的在线人数
        sendAllPlayerOnlinePlayer();
        //游戏中回话关闭的话，直接走游戏退出逻辑
        //如果玩家在游戏中的话
        if (onlinePlayerState.containsKey(playerId)) {
            playerToGame.remove(playerId);
            for (Map.Entry<Integer, Integer> entry : onlinePlayerState.entrySet()) {
                if (Objects.equals(entry.getKey(), playerId)) {
                    entry.setValue(WebSocketConstant.ONLINE_GAME_STATE_OVER);
                }
            }
        }
        //如果玩家在匹配队列里面的话也需要移除
        removePlayerfromAllReadyMatch(playerId);
//        onlinePlayPlayerReadyMatch.remove(playerId);
        //销毁多例Bean，释放内存,解除内部对象的引用，和外部对象的引用
        executorService = null;
        future = null;
        pongReceived = null;


    }

    @OnMessage
    public void onMessage(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.readValue(message, Map.class);
            log.info("Message:{}", message);
            switch ((Integer) map.get("type")) {
                case WebSocketConstant.RECEIVE_MESSAGE_FROM_PLAYER_TO_PLAYER:
                    SendMessageDTO sendMessageDTO = objectMapper.readValue(message, SendMessageDTO.class);
                    log.info("message: {}", sendMessageDTO);
                    sendMessageFromPlayerToPlayer(sendMessageDTO);
                    break;
                case WebSocketConstant.GET_ONLINE_PLAYER_MESSAGE:
                    sendOnlinePlayers(session);
                    break;
                case WebSocketConstant.MESSAGE_FROM_PLAYER_TO_GROUP:
                    GroupMessage groupMessage = objectMapper.readValue(message, GroupMessage.class);
                    log.info("groupMessage: {}", groupMessage);
                    //将消息转发一下
                    sendToAnotherGroupPlayers(groupMessage);
                    //储存历史消息
                    userService.insertGroupHistoryMessage(groupMessage);
                    break;
                //游戏匹配，等待匹配对手，进入匹配队列
                case WebSocketConstant.ONLINE_PLAY_SEARCH_ANOTHER_PLAYER:
                    //TODO 前端请加上难度参数
                    PlayerSearchDTO playerSearchDTO = JsonUtil.fromJson(message, PlayerSearchDTO.class);
                    log.info("有玩家请求匹配:{}", playerSearchDTO);
                    addPlayerTodifferentTypeGameMatchList(playerSearchDTO);

                    break;
                //游戏结束
                case WebSocketConstant.ONLINE_GAME_STATE_OVER:
                    //有玩家的游戏结束，应该修改它的游戏状态，此时玩家还需判断另一方玩家游戏是否结束
                    OnlinePlayGameOver onlinePlayGameOver = JsonUtil.fromJson(message, OnlinePlayGameOver.class);
                    log.info("onlinePlayGameOver:{}", onlinePlayGameOver);
                    playerGameOver(onlinePlayGameOver);
                    break;
                //玩家正在游戏中，需要不断提交分数数据(对应玩家提交分数，还需要把分数返还给玩家)
                case WebSocketConstant.ONLINE_PLAY_ON:
                    GameScoreDTO gameScoreDTO = JsonUtil.fromJson(message, GameScoreDTO.class);
                    log.info("玩家上传数据：{}", gameScoreDTO);
                    changOnPlayPlayerGameScore(gameScoreDTO);
                    break;
                //玩家取消匹配信息
                case WebSocketConstant.CANCEL_ONLINE_PLAY_SEARCH_ANOTHER_PLAYER:
                    log.info("玩家取消匹配");
                    //前端需要修改请求逻辑
                    PlayerCancelSearch playerCancelSearch = JsonUtil.fromJson(message, PlayerCancelSearch.class);
                    removePlayerTodifferentTypeGameMatchList(playerCancelSearch);
//                    onlinePlayPlayerReadyMatch.remove(playerId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void addPlayerTodifferentTypeGameMatchList(PlayerSearchDTO playerSearchDTO) {
        switch (playerSearchDTO.getDifficulty()) {
            case 1:
                easyOnlinePlayPlayerReadyMatch.add(playerSearchDTO.getPlayerId());
                break;
            case 2:
                normalOnlinePlayPlayerReadyMatch.add(playerSearchDTO.getPlayerId());
                break;
            case 3:
                difficultyOnlinePlayPlayerReadyMatch.add(playerSearchDTO.getPlayerId());
                break;
            default:
                log.info("前端传递过来的游戏难度参数有错误");
                break;
        }

    }

    private void removePlayerTodifferentTypeGameMatchList(PlayerCancelSearch playerCancelSearch) {
        switch (playerCancelSearch.getDifficulty()) {
            case 1:
                easyOnlinePlayPlayerReadyMatch.remove(playerCancelSearch.getPlayerId());
                break;
            case 2:
                normalOnlinePlayPlayerReadyMatch.remove(playerCancelSearch.getPlayerId());
                break;
            case 3:
                difficultyOnlinePlayPlayerReadyMatch.remove(playerCancelSearch.getPlayerId());
                break;
            default:
                log.info("前端传递过来的游戏难度参数有错误");
                break;
        }
    }

    private void removePlayerfromAllReadyMatch(Integer playerId) {
        easyOnlinePlayPlayerReadyMatch.remove(playerId);
        normalOnlinePlayPlayerReadyMatch.remove(playerId);
        difficultyOnlinePlayPlayerReadyMatch.remove(playerId);
    }

    private void changOnPlayPlayerGameScore(GameScoreDTO gameScoreDTO) {
        //先判断一下玩家是否还在游戏状态
        log.info("执行修改函数");
        if (onlinePlayerState.get(gameScoreDTO.getPlayerId()) == WebSocketConstant.ONLINE_GAME_STATE_START) {
            //修改游戏分数
            Game game = playerToGame.get(gameScoreDTO.getPlayerId());
            if (Objects.equals(gameScoreDTO.getPlayerId(), game.getPlayer1Id())) {
                game.setPlayer1Score(gameScoreDTO.getPlayerScore());
            } else {
                game.setPlayer2Score(gameScoreDTO.getPlayerScore());
            }
        }
    }

    private void playerGameOver(OnlinePlayGameOver onlinePlayGameOver) {
        //获取一下玩家的游戏对象
        Game game = playerToGame.get(onlinePlayGameOver.getPlayerId());
        GameOverVO gameOverVO = new GameOverVO();
        gameOverVO.setType(WebSocketConstant.ONLINE_PLAY_OVER);
        gameOverVO.setGame(game);
        Integer player2Id = null;
        onlinePlayerState.remove(onlinePlayGameOver.getPlayerId());
        onlinePlayerState.put(onlinePlayGameOver.getPlayerId(), WebSocketConstant.ONLINE_GAME_STATE_OVER);
        //判断一下另一个玩家是否游戏结束
        if (onlinePlayPlayerBeginMatch.containsKey(onlinePlayGameOver.getPlayerId())) {
            player2Id = onlinePlayPlayerBeginMatch.get(onlinePlayGameOver.getPlayerId());
        } else {
            for (Integer key : onlinePlayPlayerBeginMatch.keySet()) {
                if (Objects.equals(onlinePlayPlayerBeginMatch.get(key), onlinePlayGameOver.getPlayerId())) {
                    player2Id = key;
                }
            }
        }
        //获取一下另一个玩家的session
        String player2Name = null;
        for (Map.Entry<String, Integer> entry : playerNameToId.entrySet()) {
            if (Objects.equals(entry.getValue(), player2Id)) {
                player2Name = entry.getKey();
            }
        }

        Session player2Session = sessionMap.get(player2Name);


        //另一个玩家已经结束游戏
        if (onlinePlayerState.get(player2Id) == WebSocketConstant.ONLINE_GAME_STATE_OVER) {
            //此时游戏应该结束
            try {
                log.info("try-catch");
                //向两个玩家发送游戏结束信息
                synchronized (this.session) {
                    session.getBasicRemote().sendText(JsonUtil.toJson(gameOverVO));
                }

                if (player2Session != null) {
                    synchronized (player2Session) {
                        player2Session.getBasicRemote().sendText(JsonUtil.toJson(gameOverVO));
                    }

                }
                //将两个玩家移除
                removePlayerFromPlayList(onlinePlayGameOver.getPlayerId());
                removePlayerFromPlayList(player2Id);
                //保存游戏数据

                userService.saveGameInfo(game);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            //还没有结束需要等待
        }
    }

    //结束该玩家对应的游戏
    private void removePlayerFromPlayList(Integer playerId) {
        //移除对应的游戏
        playerToGame.remove(playerId);
        //移除对应的游戏状态
        onlinePlayerState.remove(playerId);
        //从开始列表中移除
        onlinePlayPlayerBeginMatch.remove(playerId);
    }

    public void sendToAnotherGroupPlayers(GroupMessage groupMessage) throws Exception {
        final List<Player> playerChatGroupAnotherPlayers = userService.getPlayerChatGroupAnotherPlayers(groupMessage.getGroupId());
        log.info("PlayerList: {}", playerChatGroupAnotherPlayers);
        for (Player player : playerChatGroupAnotherPlayers) {
            if (sessionMap.get(player.getUserName()) != null) {
                log.info("SessionMap: {}", sessionMap);
                GroupMessage messageFromGroupPlayer = new GroupMessage();
                BeanUtils.copyProperties(groupMessage, messageFromGroupPlayer);
                messageFromGroupPlayer.setType(WebSocketConstant.MESSAGE_FROM_GROUP_TO_PLAYER);
                Session session1 = sessionMap.get(player.getUserName());
                synchronized (session1) {
                    session1.getAsyncRemote().sendText(JsonUtil.toJson(messageFromGroupPlayer));
                }

            }
        }
    }

    @OnError
    public void onError(Throwable throwable) throws IOException {
        this.session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
    }

    // 关闭 WebSocket 连接的方法
    private void noHeartbeatCloseSession(Session session) {
        try {
            log.info("Closing session due to missing pong... plyerName:{}", playerName);
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "No pong received"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 发送 ping 帧并检查是否收到 pong 响应
    private void sendPingAndCheckPong(Session session) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.getAsyncRemote().sendPing(ByteBuffer.wrap(new byte[0]));
                }
                // 如果在 5 秒内没有收到 pong，则关闭连接
                executorService.schedule(() -> {
                    if (!pongReceived.isDone()) {
                        noHeartbeatCloseSession(session);
                    } else {
                        pongReceived = new CompletableFuture<>();
                    }
                }, 5, TimeUnit.SECONDS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
