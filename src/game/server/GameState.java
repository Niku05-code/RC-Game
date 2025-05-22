package game.server;

import game.common.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState {
    private final ConcurrentMap<String, PlayerData> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final List<Collectible> collectibles = new ArrayList<>();
    private final AtomicInteger nextCollectibleId = new AtomicInteger(1);

    private volatile int gameState = GameConstants.STATE_LOBBY;
    private long gameStartTime;
    private long countdownStartTime;
    private String currentWinnerInfo = "";

    private String key(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    public synchronized PlayerData addPlayer(InetAddress address, int port) {
        String clientKey = key(address, port);
        int id = nextPlayerId.getAndIncrement();
        PlayerData player = new PlayerData(id, GameConstants.GAME_WIDTH / 2, GameConstants.GAME_HEIGHT / 2, 0);
        players.put(clientKey, player);
        clients.put(clientKey, new ClientInfo(address, port));
        GameConstants.log("Added player " + id + " from " + clientKey);

        return player;
    }

    public synchronized void updatePlayerPosition(InetAddress address, int port, int x, int y) {
        if (gameState != GameConstants.STATE_IN_GAME) {
            return;
        }
        String clientKey = key(address, port);
        PlayerData player = players.get(clientKey);
        if (player != null) {
            player.x = x;
            player.y = y;
        }
    }

    public synchronized String getGameStateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("STATE:").append(gameState).append(";");
        sb.append("TIME_LEFT:").append(getRemainingGameTime()).append(";");
        sb.append("COUNTDOWN:").append(getRemainingCountdownTime()).append(";");
        sb.append("WINNER:").append(currentWinnerInfo).append(";");

        for (PlayerData player : players.values()) {
            sb.append("P:").append(player).append(";");
        }
        for (Collectible c : collectibles) {
            sb.append("C:").append(c).append(";");
        }
        return sb.toString();
    }

    public synchronized Collection<ClientInfo> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    public synchronized void spawnCollectible() {
        Random rand = new Random();
        int x = rand.nextInt(GameConstants.GAME_WIDTH - GameConstants.PLAYER_SIZE);
        int y = rand.nextInt(GameConstants.GAME_HEIGHT - GameConstants.PLAYER_SIZE);
        collectibles.add(new Collectible(nextCollectibleId.getAndIncrement(), x, y));
    }

    public synchronized void checkCollectibles(InetAddress address, int port) {
        if (gameState != GameConstants.STATE_IN_GAME) {
            return;
        }
        String clientKey = key(address, port);
        PlayerData player = players.get(clientKey);
        if (player == null) return;

        Iterator<Collectible> iter = collectibles.iterator();
        while (iter.hasNext()) {
            Collectible c = iter.next();
            if (c.intersects(player)) {
                player.score += 1;
                iter.remove();
                spawnCollectible();
                GameConstants.log("Player " + player.id + " collected object " + c.id);
            }
        }
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized void broadcastGameState(DatagramSocket socket) throws Exception {
        String state = GameConstants.CMD_STATE + " " + getGameStateString();
        byte[] buffer = state.getBytes();
        for (ClientInfo client : getAllClients()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.address, client.port);
            socket.send(packet);
        }
    }

    public synchronized void startGameCountdown() {
        if (gameState == GameConstants.STATE_LOBBY && players.size() >= 2) {
            gameState = GameConstants.STATE_COUNTDOWN;
            countdownStartTime = System.currentTimeMillis();
            currentWinnerInfo = "";
            GameConstants.log("Game countdown started!");
        }
    }

    public synchronized void finishCountdown() {
        if (gameState == GameConstants.STATE_COUNTDOWN) {
            gameState = GameConstants.STATE_IN_GAME;
            gameStartTime = System.currentTimeMillis();
            GameConstants.log("Game started!");
        }
    }

    public synchronized void checkGameEnd() {
        if (gameState == GameConstants.STATE_IN_GAME && getRemainingGameTime() <= 0) {
            gameState = GameConstants.STATE_GAME_OVER;
            currentWinnerInfo = calculateWinnerInfo();
            GameConstants.log("Game over!");
        }
    }

    private String calculateWinnerInfo() {
        PlayerData winner = null;
        int maxScore = -1;
        for (PlayerData player : players.values()) {
            if (player.score > maxScore) {
                maxScore = player.score;
                winner = player;
            } else if (player.score == maxScore && winner != null) {
                if (player.id < winner.id) {
                    winner = player;
                }
            }
        }
        if (winner != null) {
            return winner.id + "," + winner.score;
        }
        return "NO_WINNER";
    }


    public int getGameState() {
        return gameState;
    }

    public long getRemainingGameTime() {
        if (gameState == GameConstants.STATE_IN_GAME) {
            long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
            return Math.max(0, GameConstants.GAME_DURATION_SECONDS - elapsed);
        }
        return GameConstants.GAME_DURATION_SECONDS;
    }

    public long getRemainingCountdownTime() {
        if (gameState == GameConstants.STATE_COUNTDOWN) {
            long elapsed = (System.currentTimeMillis() - countdownStartTime) / 1000;
            return Math.max(0, GameConstants.COUNTDOWN_SECONDS - elapsed);
        }
        return 0;
    }

    public synchronized void resetGame() {
        for (PlayerData player : players.values()) {
            player.score = 0;
            player.x = GameConstants.GAME_WIDTH / 2;
            player.y = GameConstants.GAME_HEIGHT / 2;
        }
        collectibles.clear();
        nextCollectibleId.set(1);
        gameState = GameConstants.STATE_LOBBY;
        gameStartTime = 0;
        countdownStartTime = 0;
        currentWinnerInfo = "";
        spawnCollectible();
        spawnCollectible();
        GameConstants.log("Game state reset.");
    }

    public static class ClientInfo {
        public final InetAddress address;
        public final int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}