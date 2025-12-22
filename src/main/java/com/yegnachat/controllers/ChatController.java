package com.yegnachat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.yegnachat.util.TranslationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML private Button settingsButton;
    @FXML private VBox chatListBox;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Label chatTitle;
    @FXML private Label chatSubtitle;

    private Stage settingsStage;

    private ChatClientSocket socket;
    private Integer activeChatId = null;
    private boolean activeChatIsGroup = false;
    private boolean translateMode = false;

    private record ChatMessage(String sender, String content) {}
    private final List<ChatMessage> chatHistory = new ArrayList<>();

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        Font.loadFont(
                getClass().getResourceAsStream("/fonts/NotoSansEthiopicVariable.ttf"),
                14
        );

        socket = Session.getSocket();
        if (socket == null)
            throw new IllegalStateException("Socket missing in session");

        socket.setOnMessage(this::handleServerMessage);

        settingsButton.setOnAction(this::openSettingsWindow);
        sendButton.setOnAction(e -> sendMessage());

        requestUserList();
    }

    // ================= REQUEST LIST =================
    private void requestUserList() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "list_users");
        msg.add("payload", new JsonObject());
        socket.send(msg);
    }

    // ================= OPEN CHATS =================
    private void openPrivateChat(int userId, String username) {
        activeChatId = userId;
        activeChatIsGroup = false;

        chatTitle.setText(username);
        chatSubtitle.setText("Private chat");

        loadHistory("private", "user_id", userId);
    }

    private void openGroupChat(int groupId, String groupName) {
        activeChatId = groupId;
        activeChatIsGroup = true;

        chatTitle.setText(groupName);
        chatSubtitle.setText("Group chat");

        loadHistory("group", "group_id", groupId);
    }

    private void loadHistory(String type, String key, int id) {
        messagesBox.getChildren().clear();
        chatHistory.clear();

        JsonObject payload = new JsonObject();
        payload.addProperty("chat_type", type);
        payload.addProperty(key, String.valueOf(id));

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "fetch_history");
        msg.add("payload", payload);

        socket.send(msg);
    }

    // ================= SEND MESSAGE =================
    @FXML
    private void sendMessage() {
        if (activeChatId == null) return;

        String text = messageField.getText();
        if (text.isBlank()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("content", text);

        if (activeChatIsGroup)
            payload.addProperty("group_id", String.valueOf(activeChatId));
        else
            payload.addProperty("receiver_id", String.valueOf(activeChatId));

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "send_message");
        msg.add("payload", payload);

        socket.send(msg);

        // Optimistic UI update
        chatHistory.add(new ChatMessage("Me", text));
        renderMessages();
        messageField.clear();
    }

    // ================= SERVER HANDLER =================
    private void handleServerMessage(JsonObject msg) {
        switch (msg.get("type").getAsString()) {

            case "list_users_response" -> Platform.runLater(() -> {
                chatListBox.getChildren().clear();

                JsonArray groups = msg.getAsJsonObject("payload").getAsJsonArray("groups");
                if (groups != null) {
                    for (var g : groups) {
                        JsonObject o = g.getAsJsonObject();
                        int id = o.get("id").getAsInt();
                        String name = o.get("name").getAsString();

                        Button btn = new Button(name + " (Group)");
                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setOnAction(e -> openGroupChat(id, name));
                        chatListBox.getChildren().add(btn);
                    }
                    chatListBox.getChildren().add(new Separator());
                }

                JsonArray users = msg.getAsJsonObject("payload").getAsJsonArray("users");
                if (users != null) {
                    for (var u : users) {
                        JsonObject o = u.getAsJsonObject();
                        int id = o.get("id").getAsInt();
                        String name = o.get("username").getAsString();

                        Button btn = new Button(name);
                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setOnAction(e -> openPrivateChat(id, name));
                        chatListBox.getChildren().add(btn);
                    }
                }
            });

            case "fetch_history_response" -> Platform.runLater(() -> {
                chatHistory.clear();
                messagesBox.getChildren().clear();

                JsonArray messages = msg.getAsJsonObject("payload").getAsJsonArray("messages");
                if (messages != null) {
                    for (var m : messages) {
                        JsonObject o = m.getAsJsonObject();
                        chatHistory.add(new ChatMessage(
                                o.get("sender").getAsString(),
                                o.get("content").getAsString()
                        ));
                    }
                }
                renderMessages();
            });

            case "send_message" -> Platform.runLater(() -> {
                JsonObject p = msg.getAsJsonObject("payload");

                String content = p.has("content")
                        ? p.get("content").getAsString()
                        : "";

                String sender = p.has("sender")
                        ? p.get("sender").getAsString()
                        : "Friend"; // fallback for private/group

                chatHistory.add(new ChatMessage(sender, content));
                renderMessages();
            });
        }
    }

    // ================= TRANSLATION =================
    @FXML
    private void toggleTranslate() {
        translateMode = !translateMode;
        renderMessages();
    }

    // ================= RENDER =================
    private void renderMessages() {
        messagesBox.getChildren().clear();

        for (ChatMessage m : chatHistory) {
            boolean mine = m.sender().equals("Me");

            if (translateMode && !mine) {
                Label placeholder = new Label("Translating...");
                HBox box = new HBox(placeholder);
                box.setStyle("-fx-alignment: center-left;");
                messagesBox.getChildren().add(box);

                new Thread(() -> {
                    String t = TranslationService.translateToAmharic(m.content());
                    Platform.runLater(() -> placeholder.setText(t));
                }).start();
            } else {
                addMessageBubble(m.sender(), m.content(), mine);
            }
        }

        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private void addMessageBubble(String sender, String text, boolean mine) {
        Label label = new Label(sender + ": " + text);
        label.setWrapText(true);
        label.setMaxWidth(450);

        HBox box = new HBox(label);
        box.setStyle(mine
                ? "-fx-alignment: center-right;"
                : "-fx-alignment: center-left;"
        );

        messagesBox.getChildren().add(box);
    }

    // ================= SETTINGS =================
    private void openSettingsWindow(ActionEvent event) {
        try {
            if (settingsStage == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/yegnachat/client/settings.fxml")
                );

                BorderPane root = loader.load();
                settingsStage = new Stage();
                settingsStage.setTitle("Settings");
                settingsStage.setScene(new Scene(root));
                settingsStage.initOwner(settingsButton.getScene().getWindow());
                settingsStage.initModality(Modality.WINDOW_MODAL);
            }
            settingsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
