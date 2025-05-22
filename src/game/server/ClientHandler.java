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

            GameConstants.log("Received from " + clientAddress + ":" + clientPort + " -> " + message);

            if (message.startsWith(GameConstants.CMD_CONNECT)) {
                handleConnect(clientAddress, clientPort);
            } else if (message.startsWith(GameConstants.CMD_MOVE)) {
                if (gameState.getGameState() == GameConstants.STATE_IN_GAME) {
                    handleMove(message, clientAddress, clientPort);
                } else {
                    GameConstants.log("Move command ignored: Game not in progress.");
                }
            } else if (message.startsWith(GameConstants.CMD_START_GAME)) {
                handleStartGame(clientAddress, clientPort);
            }
            gameState.broadcastGameState(socket);

        } catch (Exception e) {
            GameConstants.log("Error handling client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleConnect(InetAddress clientAddress, int clientPort) throws Exception {
        if (gameState.getPlayerCount() >= GameConstants.MAX_PLAYERS) {
            sendResponse(GameConstants.CMD_FULL, clientAddress, clientPort);
            return;
        }
        PlayerData player = gameState.addPlayer(clientAddress, clientPort);
        sendResponse(GameConstants.CMD_WELCOME + " " + player.id, clientAddress, clientPort);
    }

    private void handleMove(String message, InetAddress clientAddress, int clientPort) {
        String[] parts = message.split(" ");
        if (parts.length == 3) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            gameState.updatePlayerPosition(clientAddress, clientPort, x, y);
            gameState.checkCollectibles(clientAddress, clientPort);
        }
    }

    private void handleStartGame(InetAddress clientAddress, int clientPort) throws Exception {
        if (gameState.getGameState() == GameConstants.STATE_GAME_OVER) {
            gameState.resetGame();
            GameConstants.log("Game reset requested by " + clientAddress + ":" + clientPort);
            gameState.startGameCountdown();
            GameConstants.log("Start game command received after game over. Resetting and starting countdown.");
            return;
        }

        if (gameState.getGameState() == GameConstants.STATE_LOBBY && gameState.getPlayerCount() >= 2) {
            gameState.startGameCountdown();
            GameConstants.log("Start game command received from " + clientAddress + ":" + clientPort);
        } else {
            sendResponse("ERROR Not ready to start game", clientAddress, clientPort);
            GameConstants.log("Start game command rejected: Not enough players or wrong state. Current state: " + gameState.getGameState() + ", Players: " + gameState.getPlayerCount());
        }
    }

    private void sendResponse(String message, InetAddress address, int port) throws Exception {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket response = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(response);
        GameConstants.log("Sent to " + address + ":" + port + " -> " + message);
    }
}