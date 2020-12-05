package ru.gaiduk.snake.game;

import ru.gaiduk.snake.math.Vector2;

import java.util.ArrayDeque;

public class Snake {

    private int boardHeight;
    private int boardWidth;

    private ArrayDeque<Vector2> snake;

    private Vector2 direction;

    private boolean hasEatenOnThisIteration = false;

    public Snake(Vector2 headPos, int boardHeight, int boardWidth) {
        init(boardHeight, boardWidth);
        snake = new ArrayDeque<>();
        snake.add(Vector2.clamp(headPos, boardWidth, boardHeight));
    }

    public Snake(int x, int y, int boardHeight, int boardWidth) {
        init(boardHeight, boardWidth);
        snake = new ArrayDeque<>();
        snake.add(Vector2.clamp(new Vector2(x, y), boardWidth, boardHeight));
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

    public ArrayDeque<Vector2> getSegments() { return snake.clone(); }

    public Vector2 getSnakeHead() {
        return snake.getFirst();
    }

    public void move() {

        var head = snake.getFirst();
//        System.out.println("head: " + head.getX() + " " + head.getY());
        var movedHead = Vector2.clamp(new Vector2(head.getX() + direction.getX(), head.getY() + direction.getY()), boardWidth, boardHeight);
//        System.out.println("movedHead: " + movedHead.getX() + " " + movedHead.getY());

        snake.addFirst(movedHead);
        snake.pollLast();
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

    private boolean canMove(Vector2 direction) {

        if(snake.size() < 2)
        {
            return true;
        }

        var head = snake.pollFirst();
        var movedHead = new Vector2(head.getX() + direction.getX(), head.getY() + direction.getY());

        if(snake.peekFirst().equals(movedHead)) {
            snake.addFirst(head);
            return false;
        }

        snake.addFirst(head);
        return true;
    }
}
