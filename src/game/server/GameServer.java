package game.server;

import game.common.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class GameServer {
    private final GameState gameState = new GameState();
    private DatagramSocket serverSocket;
    private Timer gameTimer;

    public static void main(String[] args) {
        new GameServer().start();
    }

    public void start() {
        try {
            serverSocket = new DatagramSocket(GameConstants.SERVER_PORT);
            GameConstants.log("Server started on " + getServerInfo());
            GameConstants.log("Waiting for clients...");

            gameState.spawnCollectible();
            gameState.spawnCollectible();

            gameTimer = new Timer();
            gameTimer.scheduleAtFixedRate(new GameLoopTask(), 0, 100); // Check game state every 100ms

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                new ClientHandler(serverSocket, packet, gameState).handle();
            }
        } catch (Exception e) {
            GameConstants.log("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (gameTimer != null) {
                gameTimer.cancel();
            }
        }
    }

    private String getServerInfo() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress() + ":" + GameConstants.SERVER_PORT;
    }

    private class GameLoopTask extends TimerTask {
        @Override
        public void run() {
            try {
                int currentState = gameState.getGameState();

                if (currentState == GameConstants.STATE_COUNTDOWN) {
                    if (gameState.getRemainingCountdownTime() <= 0) {
                        gameState.finishCountdown();
                    }
                } else if (currentState == GameConstants.STATE_IN_GAME) {
                    gameState.checkGameEnd();
                }

                gameState.broadcastGameState(serverSocket);

            } catch (Exception e) {
                GameConstants.log("GameLoopTask error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}