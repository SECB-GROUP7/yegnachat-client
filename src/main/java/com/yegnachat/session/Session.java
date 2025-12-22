package com.yegnachat.session;

import com.yegnachat.net.ChatClientSocket;

public final class Session {

    private static String token;
    private static int userId;
    private static ChatClientSocket socket;

    private Session() {}

    // AUTH
    public static void setAuth(String token, int userId) {
        Session.token = token;
        Session.userId = userId;
    }

    public static String getToken() {
        return token;
    }

    public static int getUserId() {
        return userId;
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    // SOCKET
    public static void setSocket(ChatClientSocket s) {
        socket = s;
    }

    public static ChatClientSocket getSocket() {
        return socket;
    }

    // CLEAR
    public static void clear() {
        token = null;
        userId = 0;
        socket = null;
    }
}
