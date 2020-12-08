package ru.gaiduk.snake.view;

import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.network.Node;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame extends JFrame {

    private final static String APP_NAME = "Snake Window";

    private static final int GRID_SCALE = 15;

    Node node;
    private JFrame menuFrame;

    public GameFrame (Node node) {
        this.node = node;
    }

    public void init(JFrame menuFrame) {
        this.menuFrame = menuFrame;
        add(new GamePanel(node.getBoard(), GRID_SCALE));
        setSize((node.getBoardSize().getX() + 3) * GRID_SCALE, (node.getBoardSize().getY() + 3) * GRID_SCALE + 22);
        setTitle(APP_NAME);
        setResizable(false);
        //setDefaultCloseOperation(EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                menuFrame.setVisible(true);
                node.close();
            }
        });

        this.addKeyListener(new Input(node));
        setLocationRelativeTo(null);
    }

    public void display() {}
}
