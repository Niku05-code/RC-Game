package game.common;

public class PlayerData {
    public final int id;
    public int x;
    public int y;
    public int score = 0;

    public PlayerData(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return id + "," + x + "," + y + "," + score;
    }
}
