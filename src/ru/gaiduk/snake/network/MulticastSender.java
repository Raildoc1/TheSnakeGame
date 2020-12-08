package ru.gaiduk.snake.network;

import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;

public class MulticastSender {

    public static final int port = 9192;
    public static final String ip = "239.192.0.4";

    private DatagramSocket socket;
    private InetAddress group;

    private Node node;

    public MulticastSender(Node node) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        group = InetAddress.getByName(ip);
        this.node = node;
    }

    public void close () {
        socket.close();
    }

    public void sendAnnouncementMessage(SnakesProto.GameMessage msg) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length, group, port);
        ackPacket.setData(Node.obj2bytes(msg));
        socket.send(ackPacket);
    }

    public void tryToReceiveJoinMsg() {
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try{
            socket.setSoTimeout(1);
            socket.receive(packet);

            node.handleReceivedPacket(packet);

        } catch (SocketTimeoutException | SocketException e){} catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
