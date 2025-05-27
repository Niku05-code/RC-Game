package game.common;

public class GameConstants {
    public static final int SERVER_PORT = 12345;
    public static final String SERVER_ADRESS = "161.97.122.183";
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
    public static final String CMD_START_GAME = "START_GAME";
    public static final int STATE_LOBBY = 0;
    public static final int STATE_COUNTDOWN = 1;
    public static final int STATE_IN_GAME = 2;
    public static final int STATE_GAME_OVER = 3;

    public static final int GAME_DURATION_SECONDS = 60;
    public static final int COUNTDOWN_SECONDS = 3;

    public static void log(String message) {
        System.out.println("[GAME] " + message);
    }
}