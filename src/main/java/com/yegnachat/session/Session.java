package com.yegnachat.session;

public final class Session {

    private static String token;
    private static int userId;

    private Session() {}

    public static void setToken(String t) {
        token = t;
    }

    public static String getToken() {
        return token;
    }

    public static void setUserId(int id) {
        userId = id;
    }

    public static int getUserId() {
        return userId;
    }

    public static boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public static void clear() {
        token = null;
        userId = 0;
    }
}
