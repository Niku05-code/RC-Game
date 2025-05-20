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

    private String key(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    public synchronized PlayerData addPlayer(InetAddress address, int port) {
        String clientKey = key(address, port);
        int id = nextPlayerId.getAndIncrement();
        PlayerData player = new PlayerData(id, GameConstants.GAME_WIDTH / 2, GameConstants.GAME_HEIGHT / 2);
        players.put(clientKey, player);
        clients.put(clientKey, new ClientInfo(address, port));
        GameConstants.log("Added player " + id + " from " + clientKey);
        return player;
    }

    public synchronized void updatePlayerPosition(InetAddress address, int port, int x, int y) {
        String clientKey = key(address, port);
        PlayerData player = players.get(clientKey);
        if (player != null) {
            player.x = x;
            player.y = y;
            GameConstants.log("Updated player " + player.id + " position to (" + x + "," + y + ")");
        }
    }

    public synchronized String getGameState() {
        StringBuilder sb = new StringBuilder();
        for (PlayerData player : players.values()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(player);
        }
        return sb.toString();
    }

    public synchronized Collection<ClientInfo> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized void broadcastGameState(DatagramSocket socket) throws Exception {
        String state = GameConstants.CMD_STATE + " " + getGameState();
        byte[] buffer = state.getBytes();
        for (ClientInfo client : getAllClients()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, client.address, client.port);
            socket.send(packet);
            GameConstants.log("Broadcasted to " + client.address + ":" + client.port + " -> " + state);
        }
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
