package ru.gaiduk.snake.view;

import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.network.Node;

import javax.swing.*;

public class GameFrame extends JFrame {

    private final static String APP_NAME = "Snake Window";

    private static final int GRID_SCALE = 15;

    Node node;

    public GameFrame (Node node) {
        this.node = node;
    }

    public void init() {
        add(new GamePanel(node.getBoard(), GRID_SCALE));
        setSize((node.getBoardSize().getX() + 3) * GRID_SCALE, (node.getBoardSize().getY() + 3) * GRID_SCALE + 22);
        setTitle(APP_NAME);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.addKeyListener(new Input(node));
        setLocationRelativeTo(null);
    }

    public void display() {}
}
