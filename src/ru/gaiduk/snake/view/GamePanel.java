package ru.gaiduk.snake.view;

import ru.gaiduk.snake.game.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel implements ActionListener, IUpdatable {

    private Board board;

    private boolean x_dir = true;
    private boolean y_dir = true;

    private int test_x = 0;
    private int test_y = 0;
    private int hScore = 0;
    //private Timer timer;
    private final int DELAY = 25;

    private int grid_scale;

    private JTextArea textArea;

    public GamePanel(Board board, int grid_scale, JTextArea textArea) {
        this.textArea = textArea;
        this.board = board;
        setBackground(Color.BLACK);
        this.grid_scale = grid_scale;
        board.registerUpdateCallback(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(!board.isActive()) {
            return;
        }

        g.setColor(new Color(226, 226, 226));
        g.fillRect(0,0, grid_scale*(board.getWidth() + 1), grid_scale);
        g.fillRect(0,grid_scale, grid_scale, grid_scale*(board.getHeight()));
        g.fillRect(grid_scale*(board.getWidth() + 1),0, grid_scale, grid_scale*(board.getHeight() + 2));
        g.fillRect(0,grid_scale*(board.getHeight() + 1), grid_scale*(board.getWidth() + 2), grid_scale);
        g.setColor(new Color(156, 200, 90));

        for (var snake : board.getSnakes()) {

            if(snake.getPlayerId() == board.getBoardOwnerPlayerId()) {
                continue;
            }

            for (var segment : snake.getSegments()) {
                if(segment.equals(snake.getSnakeHead())) g.setColor(new Color(0, 178, 255));
                else g.setColor(new Color(0, 196, 200));
                g.fillRect((segment.getX() + 1) * grid_scale,(segment.getY() + 1) * grid_scale, grid_scale, grid_scale);
            }
        }

        if(board.getBoardOwnerSnake() != null) {
            for (var segment : board.getBoardOwnerSnake().getSegments()) {
                if(segment.equals(board.getBoardOwnerSnake().getSnakeHead())) g.setColor(new Color(197, 255, 0));
                else g.setColor(new Color(156, 200, 90));
                g.fillRect((segment.getX() + 1) * grid_scale,(segment.getY() + 1) * grid_scale, grid_scale, grid_scale);
            }
        }

        g.setColor(new Color(200, 73, 51));

        for (var food : board.getFood()) {
            g.fillRect((food.getX() + 1) * grid_scale,(food.getY() + 1) * grid_scale, grid_scale, grid_scale);
        }


        textArea.setText("");
        for (var player : board.getGamePlayers().getPlayersList()) {
            textArea.append(player.getId() + " " + player.getName() + "..............." + player.getScore() + "\n");
        }

        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void actionPerformed(ActionEvent e) { }

    @Override
    public void Update() { repaint(); }
}
