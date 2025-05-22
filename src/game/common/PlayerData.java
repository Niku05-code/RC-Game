package game.common;

public class PlayerData {
    public final int id;
    public int x;
    public int y;
    public int score;

    public PlayerData(int id, int x, int y, int score) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = score;
    }

    @Override
    public String toString() {
        return id + "," + x + "," + y + "," + score;
    }
}
