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

    private final JButton startGameButton;
    private int currentGameState = GameConstants.STATE_LOBBY;
    private long remainingGameTime = GameConstants.GAME_DURATION_SECONDS;
    private long remainingCountdown = 0;

    private String winnerInfo = "";

    public GamePanel() {
        setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);

        setLayout(null);

        startGameButton = new JButton("Start Game");
        startGameButton.setBounds((GameConstants.GAME_WIDTH - 150) / 2, (GameConstants.GAME_HEIGHT - 50) / 2, 150, 50);
        startGameButton.setFont(new Font("Arial", Font.BOLD, 20));
        startGameButton.addActionListener(e -> {
            if (networkClient != null) {
                networkClient.sendStartGame();
                startGameButton.setEnabled(false);
            }
        });
        add(startGameButton);
        updateStartButtonState();

        new Timer(1000 / 60, e -> {
            if (currentGameState == GameConstants.STATE_IN_GAME) {
                updatePlayerPosition();
            }
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
        Map<Integer, PlayerData> newPlayers = new HashMap<>();
        List<Collectible> newCollectibles = new ArrayList<>();

        int parsedGameState = currentGameState;
        long parsedRemainingGameTime = remainingGameTime;
        long parsedRemainingCountdown = remainingCountdown;
        String parsedWinnerInfo = winnerInfo;

        String[] tokens = state.split(";");
        for (String token : tokens) {
            try {
                if (token.startsWith("STATE:")) {
                    parsedGameState = Integer.parseInt(token.substring(6));
                } else if (token.startsWith("TIME_LEFT:")) {
                    parsedRemainingGameTime = Long.parseLong(token.substring(10));
                } else if (token.startsWith("COUNTDOWN:")) {
                    parsedRemainingCountdown = Long.parseLong(token.substring(10));
                } else if (token.startsWith("WINNER:")) {
                    parsedWinnerInfo = token.substring(7);
                }
                else if (token.startsWith("P:")) {
                    String[] parts = token.substring(2).split(",");
                    int id = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int score = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                    newPlayers.put(id, new PlayerData(id, x, y, score));
                } else if (token.startsWith("C:")) {
                    String[] parts = token.substring(2).split(",");
                    int id = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    newCollectibles.add(new Collectible(id, x, y));
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Number format error parsing token: " + token + " - " + nfe.getMessage());
            } catch (Exception e) {
                System.err.println("Error parsing token: " + token + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        int finalParsedGameState = parsedGameState;
        long finalParsedRemainingGameTime = parsedRemainingGameTime;
        long finalParsedRemainingCountdown = parsedRemainingCountdown;
        String finalParsedWinnerInfo = parsedWinnerInfo;
        SwingUtilities.invokeLater(() -> {
            synchronized(players) {
                players.clear();
                players.putAll(newPlayers);
            }
            synchronized(collectibles) {
                collectibles.clear();
                collectibles.addAll(newCollectibles);
            }

            this.currentGameState = finalParsedGameState;
            this.remainingGameTime = finalParsedRemainingGameTime;
            this.remainingCountdown = finalParsedRemainingCountdown;
            this.winnerInfo = finalParsedWinnerInfo;

            updateStartButtonState();
            revalidate();
            repaint();
        });
    }

    private void updateStartButtonState() {
        if (currentGameState == GameConstants.STATE_LOBBY) {
            startGameButton.setText("Start Game");
            startGameButton.setVisible(true);
            startGameButton.setEnabled(players.size() >= 2);
            startGameButton.setBounds((GameConstants.GAME_WIDTH - 150) / 2, (GameConstants.GAME_HEIGHT - 50) / 2, 150, 50);
        } else if (currentGameState == GameConstants.STATE_GAME_OVER) {
            startGameButton.setText("Play Again");
            startGameButton.setVisible(true);
            startGameButton.setEnabled(true);
            startGameButton.setBounds((GameConstants.GAME_WIDTH - 200) / 2, (GameConstants.GAME_HEIGHT - 50) / 2 + 150, 200, 50);
        } else {
            startGameButton.setVisible(false);
            startGameButton.setEnabled(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(230, 240, 255),
                0, getHeight(), new Color(180, 210, 255));
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(200, 220, 240, 80));
        for (int i = 0; i < getWidth(); i += 40) {
            g2d.drawLine(i, 0, i, getHeight());
        }
        for (int j = 0; j < getHeight(); j += 40) {
            g2d.drawLine(0, j, getWidth(), j);
        }

        List<Collectible> collectiblesCopy;
        synchronized(collectibles) {
            collectiblesCopy = new ArrayList<>(collectibles);
        }
        for (Collectible c : collectiblesCopy) {
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(c.x + 2, c.y + 3, 20, 20);
            GradientPaint collectibleGradient = new GradientPaint(
                    c.x, c.y, new Color(114, 114, 114),
                    c.x + 20, c.y + 20, new Color(255, 120, 0)
            );
            g2d.setPaint(collectibleGradient);
            g2d.fillOval(c.x, c.y, 20, 20);
            g2d.setColor(new Color(255, 255, 255, 120));
            g2d.fillOval(c.x + 5, c.y + 5, 8, 8);
        }

        Map<Integer, PlayerData> playersCopy;
        synchronized(players) {
            playersCopy = new HashMap<>(players);
        }
        for (PlayerData p : playersCopy.values()) {
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fillOval(p.x + 3, p.y + 5,
                    GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);

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

            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(baseColor.darker().darker());
            g2d.drawOval(p.x, p.y,
                    GameConstants.PLAYER_SIZE, GameConstants.PLAYER_SIZE);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String name = p.id == playerId ? "YOU" : "P" + p.id;
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(name);
            g2d.drawString(name,
                    p.x + (GameConstants.PLAYER_SIZE - nameWidth)/2,
                    p.y + GameConstants.PLAYER_SIZE/2 + 5);
        }

        PlayerData me = playersCopy.get(playerId);
        if (me != null) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(5, 5, 120, 30, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + me.score, 15, 25);
        }

        if (currentGameState == GameConstants.STATE_IN_GAME) {
            g2d.setColor(new Color(0, 0, 0, 150));
            String timeStr = String.format("%02d:%02d", remainingGameTime / 60, remainingGameTime % 60);
            Font timeFont = new Font("Arial", Font.BOLD, 24);
            int timeWidth = g2d.getFontMetrics(timeFont).stringWidth(timeStr);
            g2d.fillRoundRect(getWidth() - timeWidth - 20, 5, timeWidth + 15, 30, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(timeFont);
            g2d.drawString(timeStr, getWidth() - timeWidth - 10, 28);
        }

        if (currentGameState == GameConstants.STATE_COUNTDOWN) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 100));
            String countdownText = String.valueOf(remainingCountdown);
            if (remainingCountdown <= 0) {
                countdownText = "GO!";
            }
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(countdownText)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(countdownText, x, y);
        }

        if (currentGameState == GameConstants.STATE_GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            String gameOverText = "GAME OVER!";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(gameOverText)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 50;
            g2d.drawString(gameOverText, x, y);

            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String winnerText = "Winner: ";

            if (winnerInfo != null && !winnerInfo.isEmpty() && !winnerInfo.equals("NO_WINNER")) {
                try {
                    String[] winnerParts = winnerInfo.split(",");
                    int winnerId = Integer.parseInt(winnerParts[0]);
                    int winnerScore = Integer.parseInt(winnerParts[1]);
                    winnerText += (winnerId == playerId ? "YOU" : "P" + winnerId) + " with " + winnerScore + " points!";
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Error parsing winner info in paintComponent: " + winnerInfo + " - " + e.getMessage());
                    winnerText += "Error determining winner.";
                }
            } else {
                winnerText += "No winner found!";
            }

            fm = g2d.getFontMetrics();
            x = (getWidth() - fm.stringWidth(winnerText)) / 2;
            y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + 50;
            g2d.drawString(winnerText, x, y);
        }
    }

    private void updatePlayerPosition() {
        PlayerData self = players.get(playerId);
        if (self == null) return;

        int newX;
        int newY;

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

        newX = self.x + dx;
        newY = self.y + dy;

        newX = Math.max(0, Math.min(newX, GameConstants.GAME_WIDTH - GameConstants.PLAYER_SIZE));
        newY = Math.max(0, Math.min(newY, GameConstants.GAME_HEIGHT - GameConstants.PLAYER_SIZE));

        if (newX != self.x || newY != self.y) {
            self.x = newX;
            self.y = newY;
            if (networkClient != null) {
                networkClient.sendMove(self.x, self.y);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (currentGameState == GameConstants.STATE_IN_GAME) {
            pressedKeys.add(e.getKeyCode());
        }
    }

    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    public void keyTyped(KeyEvent e) {}
}