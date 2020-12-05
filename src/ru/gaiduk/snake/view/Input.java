package ru.gaiduk.snake.view;

import ru.gaiduk.snake.game.Board;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Input extends KeyAdapter {
    Board board;

    public Input(Board board) {
        this.board = board;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        //System.out.println(event.getKeyCode());
        if (event.getKeyCode() == 87) board.changeDirection(0, -1); // W
        if (event.getKeyCode() == 83) board.changeDirection(0, 1); // S
        if (event.getKeyCode() == 68) board.changeDirection(1, 0); // D
        if (event.getKeyCode() == 65) board.changeDirection(-1 ,0); // A
    }
}
