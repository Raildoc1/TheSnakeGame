package ru.gaiduk.snake.view;

import ru.gaiduk.snake.network.Node;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class Input extends KeyAdapter {
    Node node;

    public Input(Node node) {
        this.node = node;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        try {
            if (event.getKeyCode() == 87) node.changeDirection(0, -1); // W
            if (event.getKeyCode() == 83) node.changeDirection(0, 1); // S
            if (event.getKeyCode() == 68) node.changeDirection(1, 0); // D
            if (event.getKeyCode() == 65) node.changeDirection(-1 ,0); // A
            if (event.getKeyCode() == 81) node.printKeyPoints(); // Q
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
