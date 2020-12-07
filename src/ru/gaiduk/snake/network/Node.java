package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.math.Vector2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class Node {

    private class Connection {
        InetAddress address;
        int port;
        public Connection(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    private SnakesProto.NodeRole nodeRole;
    private SnakesProto.GameConfig gameConfig;
    private Board board;

    private Timer timer;
    private TimerTask announcementMsgTimerTask;
    private TimerTask gameUpdateTimerTask;

    private int deltaTimeMillis = 1000;
    private int delayMillis = 500;

    private MulticastSender sender;

    public Node() {
        gameConfig = SnakesProto.GameConfig.newBuilder().setStateDelayMs(100).setDeadFoodProb(.5f).build();
        board = new Board(gameConfig);
    }

    public void startNewGame() throws SocketException, UnknownHostException {
        nodeRole = SnakesProto.NodeRole.MASTER;
        board.start();

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
                // TODO: send game state
            }
        };

        timer.schedule(announcementMsgTimerTask, delayMillis, deltaTimeMillis);
        timer.schedule(gameUpdateTimerTask, delayMillis, gameConfig.getStateDelayMs());
    }

    public void changeDirection(int x, int y) {
        board.changeDirection(x, y);
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

    }

    public void printKeyPoints() {
        board.printKeyPoints();
    }

}
