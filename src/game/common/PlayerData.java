package game.common;
import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public final int id;
    public int x;
    public int y;

    public PlayerData(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("%d,%d,%d", id, x, y);
    }

    public static PlayerData fromString(String data) {
        String[] parts = data.split(",");
        return new PlayerData(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        );
    }

    public static List<PlayerData> parseList(String data) {
        List<PlayerData> players = new ArrayList<>();
        if (data == null || data.trim().isEmpty()) return players;

        String[] playerStrings = data.split(" ");
        for (String playerStr : playerStrings) {
            try {
                players.add(fromString(playerStr));
            } catch (Exception e) {
                GameConstants.log("Error parsing player: " + playerStr);
            }
        }
        return players;
    }
}
