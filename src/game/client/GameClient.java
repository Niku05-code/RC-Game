package game.client;

import game.common.GameConstants;
import javax.swing.*;

public class GameClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("Multiplayer Game");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                NetworkClient networkClient = new NetworkClient("127.0.0.1"); // localhost pentru testare
                GamePanel gamePanel = new GamePanel();
                networkClient.setGamePanel(gamePanel);
                gamePanel.setNetworkClient(networkClient);

                frame.add(gamePanel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                networkClient.start();
            } catch (Exception e) {
                GameConstants.log("Client init failed: " + e.getMessage());
            }
        });
    }
}
