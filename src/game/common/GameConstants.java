package game.common;

public class GameConstants {
    public static final int SERVER_PORT = 12345;
    public static final int MAX_PLAYERS = 4;
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    public static final int PLAYER_SIZE = 40;
    public static final int MOVE_SPEED = 5;
    public static final String CMD_CONNECT = "CONNECT";
    public static final String CMD_MOVE = "MOVE";
    public static final String CMD_WELCOME = "WELCOME";
    public static final String CMD_STATE = "STATE";
    public static final String CMD_FULL = "FULL";



    public static void log(String message) {
        System.out.println("[GAME] " + message);
    }
}