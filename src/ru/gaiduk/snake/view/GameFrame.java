package ru.gaiduk.snake.view;

import ru.gaiduk.snake.game.Board;
import ru.gaiduk.snake.network.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame extends JFrame {

    private final static String APP_NAME = "Snake Window";

    public static final int GRID_SCALE = 15;
    public static final int TEXT_AREA_WIDTH = 500;

    Node node;
    private JFrame menuFrame;

    public GameFrame (Node node) {
        this.node = node;
    }

    public void init(JFrame menuFrame) {
        this.menuFrame = menuFrame;

        JTextArea textArea = new JTextArea();
        textArea.setBounds((node.getBoardSize().getX() + 3) * GRID_SCALE, 0, TEXT_AREA_WIDTH, (node.getBoardSize().getY() + 3) * GRID_SCALE);

        textArea.setEditable(false);
        textArea.setFocusable(false);

        JPanel gamePanel = new GamePanel(node.getBoard(), GRID_SCALE, textArea);

        gamePanel.setSize((node.getBoardSize().getX() + 3) * GRID_SCALE, (node.getBoardSize().getY() + 3) * GRID_SCALE);

        //add();
        setSize((node.getBoardSize().getX() + 3) * GRID_SCALE + TEXT_AREA_WIDTH, (node.getBoardSize().getY() + 3) * GRID_SCALE + 22);
        setTitle(APP_NAME);
        setResizable(false);
        //setDefaultCloseOperation(EXIT_ON_CLOSE);

        gamePanel.setLayout(null);

        //gamePanel.addKeyListener(new Input(node));

        gamePanel.add(textArea);


        //panel.setSize(TEXT_AREA_WIDTH, (node.getBoardSize().getY() + 3) * GRID_SCALE);

        //panel.setBounds((node.getBoardSize().getX() + 3) * GRID_SCALE, 0, TEXT_AREA_WIDTH, (node.getBoardSize().getY() + 3) * GRID_SCALE);

        //gamePanel.add(panel);

        add(gamePanel);

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
