package game.client;

import game.common.*;

import javax.swing.*;

public class GameClient {
    public static void main(String[] args) throws Exception {
        GameConstants.log("Starting game client...");
        String serverIp = "127.0.0.1";
        int serverPort = GameConstants.SERVER_PORT;

        GamePanel panel = new GamePanel();

        NetworkClient client = new NetworkClient(serverIp, serverPort, panel);
        panel.setNetworkClient(client);

        client.start();

        JFrame frame = new JFrame("Multiplayer Game");
        frame.setContentPane(panel);
        frame.setSize(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}