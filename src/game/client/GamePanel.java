package game.client;

import game.common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel implements KeyListener {
    private final Map<Integer, PlayerData> players = new ConcurrentHashMap<>();
    private int playerId;
    private NetworkClient networkClient;
    private final List<Collectible> collectibles = new CopyOnWriteArrayList<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    public GamePanel() {
        setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);
        new Timer(1000 / 60, e -> {
            updatePlayerPosition();
            repaint();
        }).start();
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public void updateGameState(String state) {
        System.out.println("Raw state: " + state); // Debug

        Map<Integer, PlayerData> newPlayers = new HashMap<>();
        List<Collectible> newCollectibles = new ArrayList<>();

        String[] tokens = state.split(";");
        for (String token : tokens) {
            try {
                if (token.startsWith("P:")) {
                    String[] parts = token.substring(2).split(",");
                    int id = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int score = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                    newPlayers.put(id, new PlayerData(id, x, y, score));
                }
                else if (token.startsWith("C:")) {
                    String[] parts = token.substring(2).split(",");
                    int id = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    newCollectibles.add(new Collectible(id, x, y));
                }
            } catch (Exception e) {
                System.err.println("Error parsing token: " + token);
                e.printStackTrace();
            }
        }

        synchronized(players) {
            players.clear();
            players.putAll(newPlayers);
        }

        synchronized(collectibles) {
            collectibles.clear();
            collectibles.addAll(newCollectibles);
        }

        System.out.println("Updated players: " + players.size()); // Debug
        System.out.println("Updated collectibles: " + collectibles.size()); // Debug
    }
    @Override
    protected void paintComponent(Graphics g) {
        // Folosim double buffering nativ mai eficient
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Setări de calitate
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fundal gradient
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(230, 240, 255),
                0, getHeight(), new Color(180, 210, 255));
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Grid subtil
        g2d.setColor(new Color(200, 220, 240, 80));
        for (int i = 0; i < getWidth(); i += 40) {
            g2d.drawLine(i, 0, i, getHeight());
        }
        for (int j = 0; j < getHeight(); j += 40) {
            g2d.drawLine(0, j, getWidth(), j);
        }

        // Copii sincronizate
        Map<Integer, PlayerData> playersCopy;
        List<Collectible> collectiblesCopy;

        synchronized(players) {
            playersCopy = new HashMap<>(players);
        }

        synchronized(collectibles) {
            collectiblesCopy = new ArrayList<>(collectibles);
        }

        // Desenare colectibile
        for (Collectible c : collectiblesCopy) {
            // Umbra
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(c.x + 2, c.y + 3, 20, 20);

            // Corp
            GradientPaint collectibleGradient = new GradientPaint(
                    c.x, c.y, new Color(255, 200, 0),
                    c.x + 20, c.y + 20, new Color(255, 120, 0)
            );
            g2d.setPaint(collectibleGradient);
            g2d.fillOval(c.x, c.y, 20, 20);

            // Highlight
            g2d.setColor(new Color(255, 255, 255, 120));
            g2d.fillOval(c.x + 5, c.y + 5, 8, 8);
        }

        // Desenare jucători
        PlayerData me = playersCopy.get(playerId);
        for (PlayerData p : playersCopy.values()) {
            // Umbra
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(p.x + 3, p.y + 5,
                    GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);

            // Corp
            Color baseColor = p.id == playerId ?
                    new Color(70, 130, 255) : new Color(255, 80, 80);

            GradientPaint playerGradient = new GradientPaint(
                    p.x, p.y, baseColor.brighter(),
                    p.x + GameConstants.PLAYER_SIZE/2f,
                    p.y + GameConstants.PLAYER_SIZE,
                    baseColor.darker()
            );
            g2d.setPaint(playerGradient);
            g2d.fillOval(p.x, p.y,
                    GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);

            // Contur
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(baseColor.darker().darker());
            g2d.drawOval(p.x, p.y,
                    GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);

            // Nume
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String name = p.id == playerId ? "YOU" : "P" + p.id;
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(name);
            g2d.drawString(name,
                    p.x + (GameConstants.PLAYER_SIZE - nameWidth)/2,
                    p.y + GameConstants.PLAYER_SIZE/2 + 5);
        }

        // Panou scor
        if (me != null) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(5, 5, 120, 30, 10, 10);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + me.score, 15, 25);
        }
    }

    private void updatePlayerPosition() {
        PlayerData self = players.get(playerId);
        if (self == null) return;

        int dx = 0;
        int dy = 0;

        if (pressedKeys.contains(KeyEvent.VK_LEFT)) dx -= GameConstants.MOVE_SPEED;
        if (pressedKeys.contains(KeyEvent.VK_RIGHT)) dx += GameConstants.MOVE_SPEED;
        if (pressedKeys.contains(KeyEvent.VK_UP)) dy -= GameConstants.MOVE_SPEED;
        if (pressedKeys.contains(KeyEvent.VK_DOWN)) dy += GameConstants.MOVE_SPEED;

        if (dx != 0 && dy != 0) {
            dx = (int) (dx / Math.sqrt(2));
            dy = (int) (dy / Math.sqrt(2));
        }

        if (dx != 0 || dy != 0) {
            self.x += dx;
            self.y += dy;

            if (networkClient != null) {
                networkClient.sendMove(self.x, self.y);
            }
        }
    }


    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
    public void keyTyped(KeyEvent e) {}
}