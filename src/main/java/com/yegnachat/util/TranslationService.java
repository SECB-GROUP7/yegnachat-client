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

    private static final Dotenv dotenv = Dotenv.load();
    private static final String TRANSLATION_URL = dotenv.get("TRANSLATION_URL");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private boolean translateMode = false;
    private String targetLanguageCode = "en";

    public void toggleMode() {
        translateMode = !translateMode;
    }

    public boolean isTranslateMode() {
        return translateMode;
    }

    public void setTargetLanguage(String langCode) {
        if (langCode != null && !langCode.isBlank()) {
            targetLanguageCode = langCode;
        }
    }

    public String getTargetLanguage() {
        return targetLanguageCode;
    }

    public String translate(String text, String targetLanguageCode) {
        if (text == null || text.isBlank()) return text;

        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = TRANSLATION_URL + "?q=" + encoded + "&target=" + targetLanguageCode;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return URLDecoder.decode(response.body().trim(), StandardCharsets.UTF_8);
            }

            System.err.println("[TRANSLATE] Translation failed: HTTP " + response.statusCode());
            System.out.println("[TRANSLATE] URL: " + url);
            System.out.println("[TRANSLATE] Response: " + response.body());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return text;
    }


}
