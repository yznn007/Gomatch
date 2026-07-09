package com.gomatch.common;

public class Protocol {
    // Client -> Server
    public static final String LOGIN        = "LOGIN";
    public static final String REGISTER     = "REGISTER";
    public static final String CREATE_ROOM  = "CREATE_ROOM";
    public static final String JOIN_ROOM    = "JOIN_ROOM";
    public static final String LEAVE_ROOM   = "LEAVE_ROOM";
    public static final String READY        = "READY";
    public static final String MOVE         = "MOVE";
    public static final String CHAT         = "CHAT";
    public static final String SURRENDER    = "SURRENDER";
    public static final String REQUEST_UNDO = "REQUEST_UNDO";
    public static final String REQUEST_DRAW = "REQUEST_DRAW";
    public static final String REPLY_UNDO   = "REPLY_UNDO";
    public static final String REPLY_DRAW   = "REPLY_DRAW";
    public static final String REQUEST_REMATCH = "REQUEST_REMATCH";
    public static final String REPLY_REMATCH   = "REPLY_REMATCH";
    public static final String CHANGE_PWD   = "CHANGE_PASSWORD";
    public static final String RANKING      = "RANKING";
    public static final String ROOM_LIST    = "ROOM_LIST";
    public static final String QUICK_MATCH  = "QUICK_MATCH";
    public static final String RANKED_MATCH = "RANKED_MATCH";
    public static final String SPECTATE     = "SPECTATE";
    public static final String LEAVE_SPECTATE = "LEAVE_SPECTATE";
    public static final String ADD_FRIEND   = "ADD_FRIEND";
    public static final String ACCEPT_FRIEND = "ACCEPT_FRIEND";
    public static final String REJECT_FRIEND = "REJECT_FRIEND";
    public static final String DELETE_FRIEND = "DELETE_FRIEND";
    public static final String FRIEND_LIST  = "FRIEND_LIST";
    public static final String FRIEND_REQUESTS = "FRIEND_REQUESTS";
    public static final String SAVE_GAME     = "SAVE_GAME";
    public static final String LOAD_GAME     = "LOAD_GAME";
    public static final String DELETE_SAVE   = "DELETE_SAVE";
    public static final String SAVE_LIST     = "SAVE_LIST";
    public static final String CHAT_HISTORY = "CHAT_HISTORY";
    public static final String WATCHER_HISTORY = "WATCHER_HISTORY";
    public static final String QUERY_RECORD = "QUERY_RECORD";
    public static final String LOGOUT = "LOGOUT";
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String HEARTBEAT_OK = "HEARTBEAT_OK";
    public static final String CREATE_AI_ROOM = "CREATE_AI_ROOM";
    public static final String AI_MATCH = "AI_MATCH";
    public static final String RECONNECT = "RECONNECT";
    // Admin commands
    public static final String BAN_USER         = "BAN_USER";
    public static final String UNBAN_USER       = "UNBAN_USER";
    public static final String FORCE_CLOSE_ROOM = "FORCE_CLOSE_ROOM";
    public static final String USER_LIST        = "USER_LIST";
    public static final String ADMIN_STATS      = "ADMIN_STATS";
    public static final String UPDATE_USER_ROLE = "UPDATE_USER_ROLE";
    // Server -> Client
    public static final String LOGIN_OK     = "LOGIN_OK";
    public static final String LOGIN_FAIL   = "LOGIN_FAIL";
    public static final String REGISTER_OK  = "REGISTER_OK";
    public static final String JOIN_OK      = "JOIN_OK";
    public static final String JOIN_FAIL    = "JOIN_FAIL";
    public static final String PLAYER_ENTER = "PLAYER_ENTER";
    public static final String PLAYER_LEAVE = "PLAYER_LEAVE";
    public static final String GAME_START   = "GAME_START";
    public static final String MOVE_SYNC    = "MOVE";
    public static final String GAME_OVER    = "GAME_OVER";
    public static final String CHAT_MSG     = "CHAT";
    public static final String PWD_CHANGED  = "PWD_CHANGED";
    public static final String RANKING_LIST = "RANKING_LIST";
    public static final String ERROR        = "ERROR";
    public static final String DISCONNECT   = "DISCONNECT";
    public static final String ROOM_CREATED     = "ROOM_CREATED";
    public static final String GAME_RECORDS     = "GAME_RECORDS";
    public static final String GAME_RECORDS_LIST = "GAME_RECORDS_LIST";
    public static final String RECORD_LIST = "RECORD_LIST";
    public static final String LOGOUT_OK    = "LOGOUT_OK";
    public static final String TURN_TIMEOUT = "TURN_TIMEOUT";
    public static final String USER_SCORE_UPDATE = "USER_SCORE_UPDATE";
    public static final String CANCEL_MATCH  = "CANCEL_MATCH";
    public static final String MATCH_TIMEOUT = "MATCH_TIMEOUT";
    public static final String RANKED_MATCH_FOUND = "RANKED_MATCH_FOUND";
    public static final String RANKED_READY = "RANKED_READY";

}


