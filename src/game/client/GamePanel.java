package game.client;

import game.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;

public class GamePanel extends JPanel implements KeyListener {
    private final Map<Integer, PlayerData> players = new HashMap<>();
    private int playerId;
    private NetworkClient networkClient;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        Timer timer = new Timer(1000 / 30, e -> repaint());
        timer.start();
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public void updateGameState(String state) {
        players.clear();
        String[] tokens = state.split(" ");
        for (String token : tokens) {
            String[] parts = token.split(",");
            int id = Integer.parseInt(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            players.put(id, new PlayerData(id, x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);
        for (PlayerData p : players.values()) {
            g.setColor(p.id == playerId ? Color.BLUE : Color.RED);
            g.fillOval(p.x, p.y, GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        PlayerData self = players.get(playerId);
        if (self == null) return;

        int step = 5;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT: self.x -= step; break;
            case KeyEvent.VK_RIGHT: self.x += step; break;
            case KeyEvent.VK_UP: self.y -= step; break;
            case KeyEvent.VK_DOWN: self.y += step; break;
        }

        if (networkClient != null) {
            networkClient.sendMove(self.x, self.y);
        }
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}