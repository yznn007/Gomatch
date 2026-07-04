package com.gomatch.common;

public class Protocol {
    public static final String SEPARATOR = "|";

    // Client -> Server
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String LEAVE_ROOM = "LEAVE_ROOM";
    public static final String MOVE = "MOVE";
    public static final String CHAT = "CHAT";
    public static final String SURRENDER = "SURRENDER";

    // Server -> Client
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String ROOM_LIST = "ROOM_LIST";
    public static final String JOIN_OK = "JOIN_OK";
    public static final String JOIN_FAIL = "JOIN_FAIL";
    public static final String PLAYER_ENTER = "PLAYER_ENTER";
    public static final String PLAYER_LEAVE = "PLAYER_LEAVE";
    public static final String GAME_START = "GAME_START";
    public static final String MOVE_SYNC = "MOVE";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String CHAT_MSG = "CHAT";
    public static final String ERROR = "ERROR";

    public static String build(String... parts) {
        return String.join(SEPARATOR, parts);
    }

    public static String[] parse(String msg) {
        return msg.split("\\" + SEPARATOR, -1);
    }
}
