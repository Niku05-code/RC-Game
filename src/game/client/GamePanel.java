package game.client;

import game.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class GamePanel extends JPanel implements KeyListener {
    private final Map<Integer, PlayerData> players = new HashMap<>();
    private int playerId;
    private NetworkClient networkClient;

    private final List<Collectible> collectibles = new ArrayList<>();

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
        collectibles.clear();
        String[] tokens = state.split(";");
        for (String token : tokens) {
            if (token.startsWith("P:")) {
                String[] parts = token.substring(2).split(",");
                int id = Integer.parseInt(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int score = Integer.parseInt(parts[3]);
                PlayerData p = new PlayerData(id, x, y);
                p.score = score;
                players.put(id, p);
            } else if (token.startsWith("C:")) {
                String[] parts = token.substring(2).split(",");
                int id = Integer.parseInt(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                collectibles.add(new Collectible(id, x, y));
            }
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);

        Map<Integer, PlayerData> playersCopy;
        synchronized(players) {
            playersCopy = new HashMap<>(players);
        }

        for (PlayerData p : playersCopy.values()) {
            g.setColor(p.id == playerId ? Color.BLUE : Color.RED);
            g.fillOval(p.x, p.y, GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);
        }

        List<Collectible> collectiblesCopy;
        synchronized(collectibles) {
            collectiblesCopy = new ArrayList<>(collectibles);
        }

        g.setColor(Color.ORANGE);
        for (Collectible c : collectiblesCopy) {
            g.fillOval(c.x, c.y, 20, 20);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        PlayerData me = players.get(playerId);
        if (me != null) {
            g.drawString("Score: " + me.score, 10, 20);
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