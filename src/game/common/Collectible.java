package game.common;

public class Collectible {
    public final int id;
    public int x, y;

    public Collectible(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return id + "," + x + "," + y;
    }

    public boolean intersects(PlayerData player) {
        return Math.abs(player.x - x) < GameConstants.PLAYER_SIZE &&
                Math.abs(player.y - y) < GameConstants.PLAYER_SIZE;
    }
}
