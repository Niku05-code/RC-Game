package game.client;

import game.common.*;

import javax.swing.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkClient {
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final GamePanel gamePanel;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public NetworkClient(String serverIp, int serverPort, GamePanel panel) throws Exception {
        this.serverAddress = InetAddress.getByName(serverIp);
        this.serverPort = serverPort;
        this.gamePanel = panel;
        this.socket = new DatagramSocket();
        GameConstants.log("NetworkClient created for server: " + serverIp);
    }

    public void start() {
        try {
            send(GameConstants.CMD_CONNECT);

            Thread listener = new Thread(() -> {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (running.get()) {
                    try {
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength()).trim();
                        handleMessage(message);
                    } catch (Exception e) {
                        GameConstants.log("Receive error: " + e.getMessage());
                    }
                }
            });
            listener.setDaemon(true);
            listener.start();
        } catch (Exception e) {
            GameConstants.log("Start error: " + e.getMessage());
        }
    }

    public void sendMove(int x, int y) {
        send(GameConstants.CMD_MOVE + " " + x + " " + y);
    }

    private void send(String message) {
        try {
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
            GameConstants.log("Sent: " + message);
        } catch (Exception e) {
            GameConstants.log("Send error: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        System.out.println("Received: " + message); // Debug

        SwingUtilities.invokeLater(() -> {
            if (message.startsWith(GameConstants.CMD_WELCOME)) {
                String[] parts = message.split(" ");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[1]);
                    System.out.println("Setting player ID: " + id);
                    gamePanel.setPlayerId(id);
                }
            }
            else if (message.startsWith(GameConstants.CMD_STATE)) {
                System.out.println("Updating game state");
                String state = message.substring(GameConstants.CMD_STATE.length()).trim();
                gamePanel.updateGameState(state);
            }
            else if (message.startsWith(GameConstants.CMD_FULL)) {
                JOptionPane.showMessageDialog(null, "Server is full!");
            }
        });
    }
}
