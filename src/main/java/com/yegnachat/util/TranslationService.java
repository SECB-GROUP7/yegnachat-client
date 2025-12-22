package com.yegnachat.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TranslationService {
    static Dotenv dotenv = Dotenv.load();
    private static final String TRANSLATION_URL = dotenv.get("TRANSLATION_URL");

    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private boolean translateMode = false;

    // Toggle translation mode
    public void toggleMode() {
        translateMode = !translateMode;
    }

    public boolean isTranslateMode() {
        return translateMode;
    }

    // Translate text to Amharic
    public static String translateToAmharic(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = TRANSLATION_URL + "?q=" + encoded + "&target=am";

            HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return URLDecoder.decode(response.body().trim(), StandardCharsets.UTF_8);
            } else {
                System.err.println("Translation failed: HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return text;
    }
}
