package com.yegnachat.session;

import com.yegnachat.net.ChatClientSocket;

public final class Session {

    private static String token;
    private static int userId;
    private static ChatClientSocket socket;
    private static String preferredLanguageCode = "en";

    private Session() {}

    // AUTH
    public static void setAuth(String token, int userId, String preferredLang) {
        Session.token = token;
        Session.userId = userId;
        Session.preferredLanguageCode =
                preferredLang != null && !preferredLang.isBlank() ? preferredLang : "en";
    }

    public static String getToken() { return token; }
    public static int getUserId() { return userId; }
    public static boolean isLoggedIn() { return token != null; }
    public static String getPreferredLanguageCode() { return preferredLanguageCode; }

    // SOCKET
    public static void setSocket(ChatClientSocket s) { socket = s; }
    public static ChatClientSocket getSocket() { return socket; }

    // CLEAR
    public static void clear() {
        token = null;
        userId = 0;
        socket = null;
        preferredLanguageCode = "en";
    }
}
