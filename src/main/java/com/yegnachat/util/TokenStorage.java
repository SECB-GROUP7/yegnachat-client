package com.yegnachat.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TokenStorage {

    private static final Path TOKEN_PATH = Path.of("data/token.json");
    private static final Gson gson = new Gson();

    public static String loadToken() {
        if (!TOKEN_PATH.toFile().exists()) return null;

        try (FileReader reader = new FileReader(TOKEN_PATH.toFile())) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            return obj.has("token") ? obj.get("token").getAsString() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveToken(String token) {
        try {
            Files.createDirectories(TOKEN_PATH.getParent()); // ensure folder exists
            JsonObject obj = new JsonObject();
            obj.addProperty("token", token);

            try (FileWriter writer = new FileWriter(TOKEN_PATH.toFile())) {
                gson.toJson(obj, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearToken() {
        try {
            Files.createDirectories(TOKEN_PATH.getParent()); // ensure folder exists
            JsonObject obj = new JsonObject();
            obj.addProperty("token", "");

            try (FileWriter writer = new FileWriter(TOKEN_PATH.toFile())) {
                gson.toJson(obj, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
