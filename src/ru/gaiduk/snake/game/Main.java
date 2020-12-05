package ru.gaiduk.snake.game;

import ru.gaiduk.snake.network.Node;
import ru.gaiduk.snake.view.GameFrame;

import java.awt.*;

public class Main {

    public static void main(String[] args) {

        Node node = new Node();

        GameFrame frame = new GameFrame(node);

        EventQueue.invokeLater(() -> {
            frame.init();
            frame.setVisible(true);
        });

        node.startNewGame();

    }
}
