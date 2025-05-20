package game.server;

import game.common.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClientHandler {
    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final GameState gameState;

    public ClientHandler(DatagramSocket socket, DatagramPacket packet, GameState gameState) {
        this.socket = socket;
        this.packet = packet;
        this.gameState = gameState;
    }

    public void handle() {
        try {
            String message = new String(packet.getData(), 0, packet.getLength()).trim();
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            if (message.startsWith(GameConstants.CMD_CONNECT)) {
                handleConnect(clientAddress, clientPort);
            } else if (message.startsWith(GameConstants.CMD_MOVE)) {
                handleMove(message, clientAddress, clientPort);
            }
        } catch (Exception e) {
            GameConstants.log("Error handling client: " + e.getMessage());
        }
    }

    private void handleConnect(InetAddress clientAddress, int clientPort) throws Exception {
        if (gameState.getPlayerCount() >= GameConstants.MAX_PLAYERS) {
            sendResponse(GameConstants.CMD_FULL, clientAddress, clientPort);
            return;
        }

        PlayerData player = gameState.addPlayer(clientAddress, clientPort);
        sendResponse(GameConstants.CMD_WELCOME + " " + player.id, clientAddress, clientPort);
        gameState.broadcastGameState(socket);
    }

    private void handleMove(String message, InetAddress clientAddress, int clientPort) throws Exception {
        String[] parts = message.split(" ");
        if (parts.length == 3) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            gameState.updatePlayerPosition(clientAddress, clientPort, x, y);

            // Verifică dacă a colectat ceva
            gameState.checkCollectibles(clientAddress, clientPort);

            gameState.broadcastGameState(socket);
        }
    }

    private void sendResponse(String message, InetAddress address, int port) throws Exception {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket response = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(response);
        GameConstants.log("Sent to " + address + ":" + port + " -> " + message);
    }
}
