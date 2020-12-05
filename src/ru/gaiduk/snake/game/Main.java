package ru.gaiduk.snake.game;

import ru.gaiduk.snake.network.Node;
import ru.gaiduk.snake.view.GameFrame;

import java.awt.*;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws SocketException, UnknownHostException {

        Node node = new Node();

        GameFrame frame = new GameFrame(node);

        EventQueue.invokeLater(() -> {
            frame.init();
            frame.setVisible(true);
        });

        node.startNewGame();

    }
}
