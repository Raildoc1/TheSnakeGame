package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.math.Vector2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {

    private static final int MAX_MSG_LENGTH = 1024;
    private static final int TIME_OUT_MILLIS = 100;

    private class Connection {
        InetAddress address;
        int port;
        long timeMillis;
        int playerId;
        String name;
        SnakesProto.NodeRole nodeRole;
        public Connection(InetAddress address, int port, long timeMillis, int playerId, SnakesProto.NodeRole role, String name) {
            this.address = address;
            this.port = port;
            this.timeMillis = timeMillis;
            this.playerId = playerId;
            this.nodeRole = role;
            this.name = name;
        }
        public void setTimeMillis(long timeMillis) {
            this.timeMillis = timeMillis;
        }
        public long getTimeMillis() {
            return timeMillis;
        }
    }

    public static <T> T as(Class<T> clazz, Object o){
        if(clazz.isInstance(o)){
            return clazz.cast(o);
        }
        return null;
    }

    private ArrayList<Connection> connections;
    private DatagramSocket socket;

    private int port;

    private SnakesProto.NodeRole nodeRole;
    private SnakesProto.GameConfig gameConfig;
    private Board board;

    private byte[] buf = new byte[MAX_MSG_LENGTH];

    private Timer timer;
    private TimerTask announcementMsgTimerTask;
    private TimerTask gameUpdateTimerTask;
    private TimerTask pingTimerTask;
    private TimerTask listenTimerTask;

    private ExecutorService executorService;

    private int deltaTimeMillis = 1000;
    private int delayMillis = 500;

    private MulticastSender sender;

    private long lastSeq = 0;

    private boolean gameConfigSet = false;

    public Node(int port) throws SocketException {
        executorService = Executors.newFixedThreadPool(1);
        timer = new Timer();
        gameConfig = SnakesProto.GameConfig.newBuilder().setStateDelayMs(100).setDeadFoodProb(.5f).build();
        board = new Board(gameConfig);
        connections = new ArrayList<>();
        this.port = port;
        socket = new DatagramSocket(port);
        socket.setSoTimeout(TIME_OUT_MILLIS);
    }

    public void connect( InetAddress ip, int port) throws IOException, ClassNotFoundException {
        nodeRole = SnakesProto.NodeRole.NORMAL;
        var joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder().setName("Not Vanya, because Vanya can be only one!").build();

        var gameMsg = SnakesProto.GameMessage.newBuilder().setJoin(joinMsg).setMsgSeq(System.currentTimeMillis()).build();

        sendObjectTo(gameMsg, ip, port);

        pingTimerTask = new TimerTask() {
            @Override
            public void run() {
                pingMaster();
            }
        };

        System.out.println("Handling ack meg...");

        if(handleAckMsg()) {
            System.out.println("Got ack msg!");
            board.start();
            executorService.submit(() -> {
                try {
                    handleGameStateMsgs();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }

        System.out.println("Connection failed!");

    }

    private void pingMaster() {
        var pingMsg = SnakesProto.GameMessage.PingMsg.newBuilder().build();
        var gameMsg = SnakesProto.GameMessage.newBuilder().setPing(pingMsg).setMsgSeq(System.currentTimeMillis()).build();

        try {
            sendObjectTo(gameMsg, InetAddress.getLocalHost(), 5000);
        } catch (IOException e) {
            /* IGNORE */
        }
    }

    private void handleGameStateMsgs() throws ClassNotFoundException {
        SnakesProto.GameMessage gameMsg;
        while(true) {
            try {
                DatagramPacket packet1 = new DatagramPacket(buf, buf.length);
                socket.receive(packet1);

                Object obj = parseObject(packet1.getData());

                if(as(SnakesProto.GameMessage.class, obj) != null) {

                    gameMsg = (SnakesProto.GameMessage)obj;

                    if(gameMsg.getMsgSeq() < lastSeq) {
                        System.out.println("Message Ignored!");
                        continue;
                    } else {
                        lastSeq = gameMsg.getMsgSeq();
                    }

                    if(gameMsg.hasState()) {
                        applyState(gameMsg.getState().getState());
                        continue;
                    }
                }
            } catch (IOException e) { /* IGNORE */}
        }
    }

    private boolean handleAckMsg() throws ClassNotFoundException {
        SnakesProto.GameMessage gameMsg;
        while(true) {
            try {
                DatagramPacket packet1 = new DatagramPacket(buf, buf.length);
                socket.receive(packet1);

                Object obj = parseObject(packet1.getData());

                if(as(SnakesProto.GameMessage.class, obj) != null) {
                    gameMsg = (SnakesProto.GameMessage)obj;
                    if(gameMsg.hasAck()) {
                        System.out.println("Joined successfully with id " + gameMsg.getReceiverId() + "!");
                        board.setOwnerSnake(gameMsg.getReceiverId());
                        return true;
                    } else if (gameMsg.hasError()) {
                        System.out.println("Error occur :c");
                        return false;
                    }
                }

                break;
            } catch (IOException e) { /* IGNORE */}
        }

        return false;
    }

    public static byte[] obj2bytes(Object obj) throws IOException {

        var bos = new ByteArrayOutputStream();

        try (var out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw e;
        }
    }

    public void startNewGame() throws SocketException, UnknownHostException {

        gameConfigSet = true;

        nodeRole = SnakesProto.NodeRole.MASTER;
        board.startNewGame();

        sender = new MulticastSender(this);

        announcementMsgTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    sendAnnouncementMsg();
                    sender.tryToReceiveJoinMsg();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        gameUpdateTimerTask = new TimerTask() {
            @Override
            public void run() {

                board.update();
                try {
                    sendGameState();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                long currentTime = System.currentTimeMillis();

                Iterator<Connection> connectionIterator = connections.iterator();

                while (connectionIterator.hasNext()) {
                    var con = connectionIterator.next();

                    if(currentTime - con.getTimeMillis() > gameConfig.getNodeTimeoutMs()) {
                        connectionIterator.remove();
                        System.out.println("Client disconnected!");
                    }
                }

            }
        };

//        listenTimerTask = new TimerTask() {
//            @Override
//            public void run() {
//                listenTick();
//            }
//        };

        timer.schedule(announcementMsgTimerTask, delayMillis, deltaTimeMillis);
        timer.schedule(gameUpdateTimerTask, delayMillis, gameConfig.getStateDelayMs());
//        timer.schedule(listenTimerTask, delayMillis, deltaTimeMillis);

        executorService.submit(() -> {
            while(true) {
                listenTick();
            }
        });

//        while (true) {
//            listenTick();
//        }
    }

    private void listenJoin() {

        System.out.println("joinListener");
    }

    private void sendGameState() throws IOException {
        if (connections.size() < 1) {
            return;
        }

        var stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder().setState(getGameState()).build();

        var gameMsg = SnakesProto.GameMessage.newBuilder().setMsgSeq(System.currentTimeMillis()).setState(stateMsg).build();

        for (var con : connections) {
            sendObjectTo(gameMsg, con.address, con.port);
        }
    }

    private void sendObjectTo(Object object, InetAddress ip, int port) throws IOException {
        byte[] buffer = new byte[MAX_MSG_LENGTH];
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length, ip, port);
        ackPacket.setData(obj2bytes(object));
        socket.send(ackPacket);
    }

    private void listenTick() {

        //System.out.println("Listen tick");

        try {

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            //System.out.println("Packet received!");

            handleReceivedPacket(packet);

        } catch (IOException | ClassNotFoundException e) { /*IGNORE*/ }
    }

    public void handleReceivedPacket(DatagramPacket packet) throws IOException, ClassNotFoundException {
        Object obj = parseObject(packet.getData());

        if(as(SnakesProto.GameMessage.class, obj) != null) {
            var gameMsg = (SnakesProto.GameMessage)obj;

            // Handle join
            if(gameMsg.hasJoin()) {
                System.out.println("Got join message from " + gameMsg.getJoin().getName() + " " + packet.getAddress() + " " + packet.getPort());

                var answerGameMsgBuilder = SnakesProto.GameMessage.newBuilder().setMsgSeq(System.currentTimeMillis());

                int newSnakeId = board.addSnake();
                if(newSnakeId < 0) {
                    var errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage("Board is full").build();
                    answerGameMsgBuilder.setError(errorMsg);
                } else {
                    connections.add(new Connection(packet.getAddress(), packet.getPort(), System.currentTimeMillis(), newSnakeId, SnakesProto.NodeRole.NORMAL, gameMsg.getJoin().getName()));

                    var ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
                    answerGameMsgBuilder.setReceiverId(newSnakeId).setAck(ackMsg);
                }

                var answerGameMsg = answerGameMsgBuilder.build();

                sendObjectTo(answerGameMsg, packet.getAddress(), packet.getPort());

                System.out.println("AckPacket sent");
            }

            // Handle steer
            if(gameMsg.hasSteer() && gameMsg.hasSenderId()) {
                board.changeDirection(gameMsg.getSteer().getDirection(), gameMsg.getSenderId());
            }

            // Handle ping
            if(gameMsg.hasPing()) {
                for (var con : connections) {
                    if(con.address.equals(packet.getAddress()) && con.port == packet.getPort()) {
                        con.setTimeMillis(System.currentTimeMillis());
                        break;
                    }
                }
            }

        }
    }

    public static Object parseObject(byte[] bytes) throws IOException, ClassNotFoundException {
        var bin = new ByteArrayInputStream(bytes);
        try (var in = new ObjectInputStream(bin)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void changeDirection(int x, int y) throws IOException {

        if(nodeRole == SnakesProto.NodeRole.MASTER) {
            board.changeOwnerDirection(x, y);
        } else {
            var steerMsg = SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(Vector2.vector2direction(new Vector2(x, y))).build();
            var gameMsg = SnakesProto.GameMessage.newBuilder().setSenderId(board.getBoardOwnerPlayerId()).setSteer(steerMsg).setMsgSeq(System.currentTimeMillis()).build();
            sendObjectTo(gameMsg,InetAddress.getLocalHost(), 5000);
        }
    }

    public Vector2 getBoardSize() {
        return new Vector2(board.getWidth(), board.getHeight());
    }

    public Board getBoard() {
        return board;
    }

    private SnakesProto.GameMessage createAnnouncementMsg() {

        var gameMsgBuilder = SnakesProto.GameMessage.newBuilder().setSenderId(port).setMsgSeq(System.currentTimeMillis());

        gameMsgBuilder.setAnnouncement(SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                .setPlayers(board.getGamePlayers())
                .setConfig(gameConfig)
                .setCanJoin(true) // TODO: check if board has 5x5 square empty
                .build());

        return gameMsgBuilder.build();
    }

    private void sendAnnouncementMsg() throws IOException {
        sender.sendAnnouncementMessage(createAnnouncementMsg());
    }

    private SnakesProto.GameMessage.StateMsg getGameStateMsg() {
        return SnakesProto.GameMessage.StateMsg.newBuilder().setState(getGameState()).build();
    }


    public SnakesProto.GameState getGameState() {

        var stateBuilder = SnakesProto.GameState.newBuilder().setStateOrder(board.getStateOrder());

        for (var snake : board.getSnakesList()) {
            stateBuilder.addSnakes(snake);
        }

        for (var food : board.getFoodList()) {
            stateBuilder.addFoods(food);
        }

        stateBuilder.setPlayers(getGamePlayers());

        stateBuilder.setConfig(gameConfig);

        return stateBuilder.build();
    }

    public SnakesProto.GamePlayers getGamePlayers() {

        List<SnakesProto.GamePlayer> players = new ArrayList<>();

        var gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();

        for (var connection : connections) {
            var gamePlayerBuilder = SnakesProto.GamePlayer.newBuilder();
            gamePlayerBuilder.setId(connection.playerId)
                    .setPort(connection.port)
                    .setRole(connection.nodeRole)
                    .setIpAddress(connection.address.toString())
                    .setName(connection.name)
                    .setScore(board.getSnake(connection.playerId).getScore());

            gamePlayersBuilder.addPlayers(gamePlayerBuilder.build());

            System.out.println("add connection");
        }

        var thisPlayerBuilder = SnakesProto.GamePlayer.newBuilder();
        thisPlayerBuilder.setId(board.getBoardOwnerPlayerId())
                .setPort(0)
                .setRole(nodeRole)
                .setIpAddress("")
                .setName("MASTER")
                .setScore(board.getBoardOwnerSnake().getScore());

        System.out.println("add last connection!");

        return gamePlayersBuilder.addPlayers(thisPlayerBuilder.build()).build();
    }

    private void applyState(SnakesProto.GameState state) {
        if(!gameConfigSet) {
            gameConfig = state.getConfig();
            timer.schedule(pingTimerTask, delayMillis, gameConfig.getPingDelayMs());
            gameConfigSet = true;
        }
        board.applyState(state);
    }

    public void printKeyPoints() {
        board.printKeyPoints();
        return;
    }

    public void close() {
        timer.cancel();
        if(sender != null) {
            sender.close();
        }
        socket.disconnect();
        socket.close();
    }

}
