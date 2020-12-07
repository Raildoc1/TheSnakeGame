package ru.gaiduk.snake.game;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.math.Vector2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

public class Snake {

    private int boardHeight;
    private int boardWidth;

    private ArrayList<Vector2> snake;

    private Vector2 direction;

    private boolean hasEatenOnThisIteration = false;
    private SnakesProto.GameState.Snake.SnakeState state = SnakesProto.GameState.Snake.SnakeState.ALIVE;

    private int playerId;

    public Snake(Vector2 headPos, int boardHeight, int boardWidth, int playerId) {
        init(boardHeight, boardWidth);
        snake = new ArrayList<>();
        snake.add(Vector2.clamp(headPos, boardWidth, boardHeight));
        this.playerId = playerId;
    }

    public Snake(int x, int y, int boardHeight, int boardWidth, int playerId) {
        init(boardHeight, boardWidth);
        snake = new ArrayList<>();
        snake.add(Vector2.clamp(new Vector2(x, y), boardWidth, boardHeight));
        this.playerId = playerId;
    }

    private void init(int boardHeight, int boardWidth) {
        System.out.println("init board size: " + boardWidth + " " + boardHeight);
        direction = new Vector2(0, 1);
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
    }

    public void ChangeDirection(Vector2 direction)
    {
        if(canMove(direction)){
            this.direction = direction;
        }
    }

    public ArrayList<Vector2> getSegments() {
        return (ArrayList<Vector2>) snake.clone();
    }

    public Vector2 getSnakeHead() {
        return snake.get(0);
    }

    public void move() {

        var head = snake.get(0);
        var movedHead = Vector2.clamp(new Vector2(head.getX() + direction.getX(), head.getY() + direction.getY()), boardWidth, boardHeight);

        snake.add(0, movedHead);
        snake.remove(snake.size() - 1);
    }

    public boolean eatOn(Vector2 pos){
        for (var segment : snake) {
            if(pos.equals(segment)) {
                snake.add(pos);
                hasEatenOnThisIteration = true;
                return true;
            }
        }
        hasEatenOnThisIteration = false;
        return false;

    }

    public boolean checkSelfCollision() {

        if(hasEatenOnThisIteration) {
            return false;
        }

        hasEatenOnThisIteration = false;

        var head = getSnakeHead();

        for (var segment : snake) {
            if(head.equals(segment) && head != segment) {
                snake.add(head);
                return true;
            }
        }

        return false;
    }

    public void die() {
        snake.clear();
    }

    private boolean canMove(Vector2 direction) {

        if(snake.size() < 2)
        {
            return true;
        }

        var head = snake.get(0);
        var movedHead = new Vector2(head.getX() + direction.getX(), head.getY() + direction.getY());

        if(snake.get(1).equals(movedHead)) {
            return false;
        }

        return true;
    }

    public SnakesProto.Direction getDirection() {
        if(direction.getX() == 1) {
            return SnakesProto.Direction.RIGHT;
        } else if (direction.getX() == -1) {
            return SnakesProto.Direction.LEFT;
        } else if(direction.getY() == 1) {
            return SnakesProto.Direction.UP;
        } else {
            return SnakesProto.Direction.DOWN;
        }
    }

    public ArrayList<Vector2> getKeyPoints() {

        if(snake.size() == 1) {
            return getSegments();
        }

        ArrayList<Vector2> result = new ArrayList<>();

        result.add(getSnakeHead());

        if(snake.size() == 2) {
            result.add(Vector2.Sub(snake.get(1), getSnakeHead()));
            return result;
        }

        Vector2 prevDirection = Vector2.Sub(snake.get(1), getSnakeHead());
        Vector2 curDirection;
        int amount = 1;

        for(int i = 1; i < snake.size() - 1; i++) {
            curDirection = Vector2.Sub(snake.get(i + 1), snake.get(i));
            if(prevDirection.equals(curDirection)) {
                amount++;
            } else {
                result.add(Vector2.Mul(prevDirection, amount));
                amount = 1;
                prevDirection = curDirection;
            }
        }

        result.add(Vector2.Mul(prevDirection, amount));

        return result;

    }

    public SnakesProto.GameState.Snake convertToProtoSnake() {
        var builder = SnakesProto.GameState.Snake.newBuilder()
                .setState(state)
                .setPlayerId(playerId)
                .setHeadDirection(getDirection());

        var head = getSnakeHead();

        for (var p : getKeyPoints()) {
            builder.addPoints(p.convertToCoord());
        }

        return builder.build();

    }

}
