package game.server;

import game.common.*;
import java.net.*;

public class GameServer {
    private final GameState gameState = new GameState();

    public static void main(String[] args) {
        new GameServer().start();
    }

    public void start() {
        try {
            DatagramSocket socket = new DatagramSocket(GameConstants.SERVER_PORT);
            GameConstants.log("Server started on " + getServerInfo());
            GameConstants.log("Waiting for clients...");

            gameState.spawnCollectible();
            gameState.spawnCollectible();

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                new ClientHandler(socket, packet, gameState).handle();
            }
        } catch (Exception e) {
            GameConstants.log("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getServerInfo() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress() + ":" + GameConstants.SERVER_PORT;
    }
}