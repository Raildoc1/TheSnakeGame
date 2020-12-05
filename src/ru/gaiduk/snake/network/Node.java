package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.math.Vector2;

public class Node {

    private SnakesProto.NodeRole nodeRole;
    private SnakesProto.GameConfig gameConfig;
    private Board board;

    public Node() {
        gameConfig = SnakesProto.GameConfig.newBuilder().setStateDelayMs(100).setDeadFoodProb(.5f).build();
        board = new Board(gameConfig);
    }

    public void startNewGame() {
        nodeRole = SnakesProto.NodeRole.MASTER;
        board.start();
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

}
