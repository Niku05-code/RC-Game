package game.client;

import game.common.*;
import javax.swing.*;
import java.net.*;
import java.util.concurrent.*;

public class NetworkClient {
    private DatagramSocket socket;
    private final String serverAddress;
    private GamePanel gamePanel;
    private volatile boolean running;

    public NetworkClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public void start() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            running = true;
            sendMessage(GameConstants.CMD_CONNECT);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(this::receiveLoop);
        } catch (Exception e) {
            showError("Client start failed: " + e.getMessage());
        }
    }

    private void receiveLoop() {
        while (running) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                processMessage(msg);
            } catch (SocketTimeoutException ignored) {} catch (Exception e) {
                stop();
            }
        }
    }

    private void processMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith(GameConstants.CMD_WELCOME)) {
                int id = Integer.parseInt(message.split(" ")[1]);
                gamePanel.setPlayerId(id);
            } else if (message.startsWith(GameConstants.CMD_STATE)) {
                String stateData = message.substring(GameConstants.CMD_STATE.length()).trim();
                gamePanel.updateGameState(PlayerData.parseList(stateData));
            } else if (message.equals(GameConstants.CMD_FULL)) {
                showError("Server is full");
                System.exit(0);
            }
        });
    }

    private void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(serverAddress), GameConstants.SERVER_PORT);
            socket.send(packet);
        } catch (Exception e) {
            GameConstants.log("Send error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        javax.swing.JOptionPane.showMessageDialog(
                null,
                message,
                "Eroare",
                javax.swing.JOptionPane.ERROR_MESSAGE
        );
    }

    public void sendMove(int x, int y) {
        sendMessage(GameConstants.CMD_MOVE + " " + x + " " + y);
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    public void setGamePanel(GamePanel panel) {
        this.gamePanel = panel;
    }
}