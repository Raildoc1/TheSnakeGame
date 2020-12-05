package ru.gaiduk.snake.game;

import ru.gaiduk.snake.view.GameFrame;

import java.awt.*;

public class Main {

    public static void main(String[] args) {

        Board board = new Board();

        GameFrame frame = new GameFrame(board);

        EventQueue.invokeLater(() -> {
            frame.init();
            frame.setVisible(true);
        });

        board.start();

    }
}
