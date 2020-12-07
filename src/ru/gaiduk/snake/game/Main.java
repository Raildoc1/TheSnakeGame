package ru.gaiduk.snake.game;

import ru.gaiduk.snake.network.Node;
import ru.gaiduk.snake.view.GameFrame;

import java.awt.*;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws IOException {

        if(args.length != 2) {
            System.out.println("Wrong arguments!");
            System.out.println("Usage: main <port> <mode>");
            return;
        }

        Node node = new Node(Integer.parseInt(args[0]));

        GameFrame frame = new GameFrame(node);

        EventQueue.invokeLater(() -> {
            frame.init();
            frame.setVisible(true);
        });

        int mode = Integer.parseInt(args[1]);

        if(mode == 0) {
            node.startNewGame();
        } else {
            node.connect();
        }

    }
}
