package ru.gaiduk.snake.view;

import me.ippolitov.fit.snakes.SnakesProto;
import ru.gaiduk.snake.network.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuFrame extends JFrame {

    private class Connection {
        InetAddress ip;
        int port;
        JButton button;
        long timeMillis;

        public Connection(InetAddress ip, int port, JButton jButton) {
            this.button = jButton;
            this.ip = ip;
            this.port = port;
            timeMillis = System.currentTimeMillis();
        }
    }

    private MulticastSocket socket;
    private InetAddress group;

    public static final int WIN_H = 500;
    public static final int WIN_W = 300;

    private JFrame gameFrame;

    private ArrayList<Connection> connectionButtons;

    private JPanel panel;
    private JButton newGameButton;

    private int port;

    private Node node;

    private java.util.Timer timer;
    private TimerTask timerTask;

    private ExecutorService executorService;

    public MenuFrame (int port, JFrame gameFrame, Node node) throws IOException {

        socket = new MulticastSocket(9192);
        group = InetAddress.getByName("239.192.0.4");
        socket.joinGroup(group);

        executorService = Executors.newFixedThreadPool(1);

        connectionButtons = new ArrayList<>();

        this.port = port;
        this.gameFrame = gameFrame;
        this.node = node;
        timer = new java.util.Timer();
    }

    public void init() {
        panel = new JPanel();

        newGameButton = new JButton("New Game");

        newGameButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gameFrame.setVisible(true);
                try {
                    node.startNewGame();
                    setVisible(false);
                } catch (SocketException ex) {
                    ex.printStackTrace();
                } catch (UnknownHostException ex) {
                    ex.printStackTrace();
                }
            }
        });

        panel.add(newGameButton);

        executorService.submit(() -> {
            while(true) {
                try {
                    receiveAvailableConnections();
                    updateButtons();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        });

        timerTask = new TimerTask() {
            @Override
            public void run() {
                checkConnections();
            }
        };

        timer.schedule(timerTask, 500, 100);

        add(panel);

        setSize(WIN_W, WIN_H);
        setTitle("Snake");
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        //this.addKeyListener(new Input(node));
        setLocationRelativeTo(null);
    }

    private void updateButtons() {
        panel.removeAll();
        panel.add(newGameButton);
        for (var b : connectionButtons) {
            panel.add(b.button);
        }

        panel.revalidate();
        panel.repaint();
    }

    private void receiveAvailableConnections() throws IOException, ClassNotFoundException {

        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        Object obj = Node.parseObject(packet.getData());

        System.out.println("Packet received!");

        if(Node.as(SnakesProto.GameMessage.class, obj) != null) {
            var gameMsg = (SnakesProto.GameMessage) obj;
            System.out.println("Game msg received!");

            if(gameMsg.hasAnnouncement()) {
                System.out.println("Announcement msg received!");

                for (var connection : connectionButtons) {
                    if(connection.ip.equals(packet.getAddress()) && connection.port == packet.getPort()) {
                        return;
                    }
                }

                JButton button = new JButton("connect to " + packet.getAddress() + " " + packet.getPort());

                button.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            System.out.println("Connecting to " + packet.getAddress() + " " + packet.getPort() + "...");
                            //node.connect(InetAddress.getLocalHost(), gameMsg.getSenderId());
                            node.connect(packet.getAddress(), packet.getPort());
                            setVisible(false);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                connectionButtons.add(new Connection(packet.getAddress(), packet.getPort(), button));
            }
        }

    }

    private void checkConnections() {
        for (var connection : connectionButtons) {
            if(System.currentTimeMillis() - connection.timeMillis > 5000) {
                connectionButtons.remove(connection);
                updateButtons();
                return;
            }
        }
    }
}
