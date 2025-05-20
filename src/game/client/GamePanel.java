package game.client;

import game.common.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class GamePanel extends JPanel {
    private int playerId = -1;
    private List<PlayerData> players;
    private NetworkClient networkClient;


    public GamePanel() {
        setPreferredSize(new Dimension(
                GameConstants.GAME_WIDTH,
                GameConstants.GAME_HEIGHT
        ));
        setBackground(Color.WHITE);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (playerId == -1) return; // not connected yet

                PlayerData me = players.stream()
                        .filter(p -> p.id == playerId)
                        .findFirst()
                        .orElse(null);

                if (me == null) return;

                int step = 10;
                int x = me.x;
                int y = me.y;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        x -= step;
                        break;
                    case KeyEvent.VK_RIGHT:
                        x += step;
                        break;
                    case KeyEvent.VK_UP:
                        y -= step;
                        break;
                    case KeyEvent.VK_DOWN:
                        y += step;
                        break;
                }


                if (networkClient != null) {
                    networkClient.sendMove(x, y);
                }
            }
        });

        GameConstants.log("GamePanel initialized");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (players != null) {
            for (PlayerData player : players) {
                g2d.setColor(player.id == playerId ? Color.BLUE : Color.RED);
                g2d.fillOval(player.x - 15, player.y - 15, 30, 30);
                g2d.setColor(Color.WHITE);
                g2d.drawString(String.valueOf(player.id), player.x - 5, player.y + 5);
            }
        } else {
            g2d.setColor(Color.BLACK);
            g2d.drawString("Connecting to server...", 20, 20);
        }
    }

    public void updateGameState(List<PlayerData> players) {
        this.players = players;
        repaint();
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }
}