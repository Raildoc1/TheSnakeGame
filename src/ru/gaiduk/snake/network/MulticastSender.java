package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;

public class MulticastSender {

    public static final int port = 9192;
    public static final String ip = "239.192.0.4";

    private DatagramSocket socket;
    private InetAddress group;
    private byte[]buf;

    long start;
    long prevTime;
    long deltaTime = 0;
    long delay = 3000;

    public MulticastSender() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        group = InetAddress.getByName(ip);
    }

    public void close () {
        socket.close();
    }

    public void SendAnnouncementMessage(SnakesProto.GameMessage msg) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length, group, port);
        ackPacket.setData(Node.obj2bytes(msg));
        socket.send(ackPacket);
    }
}
