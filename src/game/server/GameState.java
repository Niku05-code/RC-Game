package game.server;

import game.common.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState {
    private final ConcurrentMap<InetAddress, PlayerData> players = new ConcurrentHashMap<>();
    private final ConcurrentMap<InetAddress, ClientInfo> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    public synchronized PlayerData addPlayer(InetAddress address, int clientPort) {
        int id = nextPlayerId.getAndIncrement();
        PlayerData player = new PlayerData(id, GameConstants.GAME_WIDTH / 2, GameConstants.GAME_HEIGHT / 2);
        players.put(address, player);
        clients.put(address, new ClientInfo(address, clientPort));
        return player;
    }

    public synchronized void updatePlayerPosition(InetAddress address, int x, int y) {
        PlayerData player = players.get(address);
        if (player != null) {
            player.x = x;
            player.y = y;
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