package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.math.Vector2;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Node {

    private static final int MAX_MSG_LENGTH = 1024;
    private static final int TIME_OUT_MILLIS = 100;

    private class Connection {
        InetAddress address;
        int port;
        public Connection(InetAddress address, int port) {
            this.address = address;
            this.port = port;
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

    private SnakesProto.NodeRole nodeRole;
    private SnakesProto.GameConfig gameConfig;
    private Board board;

    private byte[] buf = new byte[MAX_MSG_LENGTH];

    private Timer timer;
    private TimerTask announcementMsgTimerTask;
    private TimerTask gameUpdateTimerTask;
    private TimerTask listenTimerTask;

    private int deltaTimeMillis = 1000;
    private int listenDeltaTimeMillis = 100;
    private int delayMillis = 500;

    private MulticastSender sender;

    public Node(int port) throws SocketException {
        gameConfig = SnakesProto.GameConfig.newBuilder().setStateDelayMs(100).setDeadFoodProb(.5f).build();
        board = new Board(gameConfig);
        connections = new ArrayList<>();
        socket = new DatagramSocket(port);
        socket.setSoTimeout(TIME_OUT_MILLIS);
    }

    public void connect() throws IOException, ClassNotFoundException {
        nodeRole = SnakesProto.NodeRole.NORMAL;
        var joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder().setName("Not Vanya, because Vanya can be only one!").build();

        var gameMsg = SnakesProto.GameMessage.newBuilder().setJoin(joinMsg).setMsgSeq(System.currentTimeMillis()).build();

        sendObjectTo(gameMsg, InetAddress.getLocalHost(), 5000);

        if(handleAckMsg()) {
            board.start();

            handleGameStateMsgs();

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

    public byte[] obj2bytes(Object obj) throws IOException {

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
        nodeRole = SnakesProto.NodeRole.MASTER;
        board.startNewGame();

        sender = new MulticastSender();

        timer = new Timer();
        announcementMsgTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    sendAnnouncementMsg();
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
            }
        };

        timer.schedule(announcementMsgTimerTask, delayMillis, deltaTimeMillis);
        timer.schedule(gameUpdateTimerTask, delayMillis, gameConfig.getStateDelayMs());

        while (true) {
            listenTick();
        }
    }

    private void sendGameState() throws IOException {
        if (connections.size() < 1) {
            return;
        }

        var stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder().setState(board.getGameState()).build();

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
        try {

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            Object obj = parseObject(packet.getData());

            if(as(SnakesProto.GameMessage.class, obj) != null) {
                var gameMsg = (SnakesProto.GameMessage)obj;

                if(gameMsg.hasJoin()) {
                    System.out.println("Got join message from " + gameMsg.getJoin().getName() + " " + packet.getAddress() + " " + packet.getPort());

                    var answerGameMsgBuilder = SnakesProto.GameMessage.newBuilder().setMsgSeq(System.currentTimeMillis());

                    int newSnakeId = board.addSnake();
                    if(newSnakeId < 0) {
                        var errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage("Board is full").build();
                        answerGameMsgBuilder.setError(errorMsg);
                    } else {
                        connections.add(new Connection(packet.getAddress(), packet.getPort()));

                        var ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
                        answerGameMsgBuilder.setReceiverId(newSnakeId).setAck(ackMsg);
                    }

                    var answerGameMsg = answerGameMsgBuilder.build();

                    sendObjectTo(answerGameMsg, packet.getAddress(), packet.getPort());

                    System.out.println("AckPacket sent");
                }

                if(gameMsg.hasSteer() && gameMsg.hasSenderId()) {
                    board.changeDirection(gameMsg.getSteer().getDirection(), gameMsg.getSenderId());
                }

            }

        } catch (IOException | ClassNotFoundException e) { /*IGNORE*/ }
    }

    private Object parseObject(byte[] bytes) throws IOException, ClassNotFoundException {
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

    private SnakesProto.GameMessage.AnnouncementMsg createAnnouncementMsg() {
        return SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                .setPlayers(board.getGamePlayers())
                .setConfig(gameConfig)
                .setCanJoin(true) // TODO: check if board has 5x5 square empty
                .build();
    }

    private void sendAnnouncementMsg() throws IOException {
        sender.SendAnnouncementMessage(createAnnouncementMsg());
    }

    private SnakesProto.GameMessage.StateMsg getGameStateMsg() {
        return SnakesProto.GameMessage.StateMsg.newBuilder().setState(board.getGameState()).build();
    }

    private void applyState(SnakesProto.GameState state) {
        gameConfig = state.getConfig();
        board.applyState(state);
    }

    public void printKeyPoints() {
        board.printKeyPoints();
        return;
    }

}
