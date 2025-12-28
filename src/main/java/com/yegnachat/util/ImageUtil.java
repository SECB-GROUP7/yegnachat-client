package com.yegnachat.util;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.scene.image.Image;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ImageUtil {

    private static final Dotenv dotenv = Dotenv.load();
    public static final String UPLOAD_URL = dotenv.get("UPLOAD_URL");

    private ImageUtil() {}

    public static Image loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return defaultAvatar();
        try {
            Image img = new Image(UPLOAD_URL + avatarUrl, false);
            if (img.isError()) {
                System.err.println("Error loading avatar: " + img.getException());
                return defaultAvatar();
            }
            System.out.println("Avatar loaded successfully: " + avatarUrl);
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return defaultAvatar();
        }
    }


    private static Image defaultAvatar() {
        return new Image(
                ImageUtil.class.getResourceAsStream("/icons/user.png")
        );
    }

    // New method specifically for avatar upload
    public static String uploadAvatar(File file, int userId) throws IOException {
        String mimeType = java.nio.file.Files.probeContentType(file.toPath());

        URL url = new URL(UPLOAD_URL + "/uploads");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", mimeType);
        connection.setRequestProperty("X-Purpose", "avatar");
        connection.setRequestProperty("X-Owner-Id", String.valueOf(userId));

        try (OutputStream os = connection.getOutputStream();
             InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (InputStream respStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(respStream))) {
                String resp = reader.readLine();
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(resp).getAsJsonObject();
                return obj.get("image_url").getAsString();
            }
        } else {
            throw new IOException("Avatar upload failed with code: " + responseCode);
        }
    }
    public static String detectMime(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

}
