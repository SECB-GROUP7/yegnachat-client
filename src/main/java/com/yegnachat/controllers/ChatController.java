package com.yegnachat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.yegnachat.util.TranslationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    /* ================= FXML ================= */
    @FXML
    private VBox chatListBox;
    @FXML
    private VBox messagesBox;
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button translateButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Label chatTitle;
    @FXML
    private Label chatSubtitle;
    @FXML
    private ImageView chatAvatar;
    @FXML
    private StackPane rootStack;
    @FXML
    private HBox titleHbox;
    private SettingsController settingsController;

    /* ================= STATE ================= */
    private ChatClientSocket socket;
    private Integer activeChatId = null;
    private boolean activeChatIsGroup = false;

    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private TranslationService translationService;
    private boolean translateMode = false;

    private record ChatMessage(int senderId, String senderName, String avatarUrl, String content) {
    }

    /* ================= INIT ================= */
    @FXML
    public void initialize() {
        System.out.println("[INIT] Initializing ChatController...");

        Font.loadFont(
                getClass().getResourceAsStream("/fonts/NotoSansEthiopicVariable.ttf"),
                14
        );

        socket = Session.getSocket();
        if (socket == null) throw new IllegalStateException("Socket missing");

        translationService = new TranslationService();

        socket.setOnMessage(this::handleServerMessage);

        sendButton.setOnAction(e -> sendMessage());
        translateButton.setOnAction(e -> toggleTranslate());
        settingsButton.setOnAction(e -> openSettings());
        setAvatar(null);
        requestUserList();

        titleHbox.setOnMouseClicked(e -> {
            if (activeChatIsGroup)
                openGroupInfo(activeChatId, chatTitle.getText(), "Group description here");
            else
                openChatInfo(activeChatId, chatTitle.getText(), "User bio here", "avatar.png");
        });

    }

    /* ================= AVATAR ================= */
    private Image loadAvatar(String url) {
        InputStream is;

        if (url == null || url.isBlank()) {
            is = getClass().getResourceAsStream("/icons/user.png");
        } else {
            is = getClass().getResourceAsStream(url);
            if (is == null) {
                System.out.println("[AVATAR] Could not load avatar at " + url + ", using default");
                is = getClass().getResourceAsStream("/icons/user.png");
            }
        }
        return new Image(is);
    }

    private void setAvatar(String url) {
        chatAvatar.setImage(loadAvatar(url));
    }

    /* ================= REQUEST LIST ================= */
    private void requestUserList() {
        System.out.println("[REQUEST] Sending list_users request...");
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "list_users");
        msg.add("payload", new JsonObject());
        socket.send(msg);
    }

    /* ================= OPEN CHAT ================= */
    private void openGroup(int id, String name) {
        System.out.println("[OPEN] Opening group chat: " + id + " - " + name);
        activeChatId = id;
        activeChatIsGroup = true;

        chatTitle.setText(name);
        chatSubtitle.setText("Group chat");
        setAvatar(null);

        chatHistory.clear();
        messagesBox.getChildren().clear();

        requestHistory("group", "group_id", id);
    }

    private void openPrivate(int id, String username) {
        System.out.println("[OPEN] Opening private chat: " + id + " - " + username);
        activeChatId = id;
        activeChatIsGroup = false;

        chatTitle.setText(username);
        chatSubtitle.setText("Private chat");
        setAvatar(null);

        chatHistory.clear();
        messagesBox.getChildren().clear();

        requestHistory("private", "user_id", id);
    }

    private void requestHistory(String type, String key, int id) {
        System.out.println("[REQUEST] Fetching history: " + type + " for id=" + id);
        JsonObject payload = new JsonObject();
        payload.addProperty("chat_type", type);
        payload.addProperty(key, String.valueOf(id)); // <-- convert int to string

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "fetch_history");
        msg.add("payload", payload);

        socket.send(msg);
    }


    /* ================= SEND ================= */
    private void sendMessage() {
        if (activeChatId == null) {
            System.out.println("[SEND] No active chat selected.");
            return;
        }
        if (messageField.getText().isBlank()) {
            System.out.println("[SEND] Message field is blank.");
            return;
        }

        String content = messageField.getText();
        System.out.println("[SEND] Sending message: " + content);

        JsonObject payload = new JsonObject();
        payload.addProperty("content", content);

        if (activeChatIsGroup)
            payload.addProperty("group_id", String.valueOf(activeChatId));
        else
            payload.addProperty("receiver_id", String.valueOf(activeChatId));

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "send_message");
        msg.add("payload", payload);

        chatHistory.add(new ChatMessage(Session.getUserId(), "Me", "", content));
        renderMessages();


        socket.send(msg);

        messageField.clear();
    }


    /* ================= SERVER ================= */
    private void handleServerMessage(JsonObject msg) {
        if (!Session.isLoggedIn()) {
            System.out.println("[SERVER] Message ignored (not logged in)");
            return;
        }
        String type = msg.has("type") ? msg.get("type").getAsString() : "unknown";

        if (settingsController != null) {
            settingsController.handleServerMessage(
                    type,
                    msg.getAsJsonObject("payload")
            );
        }
        switch (type) {

            case "list_users_response" -> Platform.runLater(() -> {
                System.out.println("[SERVER] Handling list_users_response...");
                chatListBox.getChildren().clear();
                JsonObject payload = msg.getAsJsonObject("payload");
                JsonArray groups = payload.getAsJsonArray("groups");
                if (groups != null) {
                    for (var g : groups) {
                        JsonObject o = g.getAsJsonObject();
                        Button b = new Button(o.get("name").getAsString());
                        b.setMaxWidth(Double.MAX_VALUE);
                        b.setOnAction(e -> openGroup(o.get("id").getAsInt(), o.get("name").getAsString()));
                        chatListBox.getChildren().add(b);
                        System.out.println("[LIST] Added group: " + o.get("name").getAsString());
                    }
                }

                JsonArray users = payload.getAsJsonArray("users");
                if (users != null) {
                    for (var u : users) {
                        JsonObject o = u.getAsJsonObject();
                        Button b = new Button(o.get("username").getAsString());
                        b.setMaxWidth(Double.MAX_VALUE);
                        b.setOnAction(e -> openPrivate(o.get("id").getAsInt(), o.get("username").getAsString()));
                        chatListBox.getChildren().add(b);
                        System.out.println("[LIST] Added user: " + o.get("username").getAsString());
                    }
                }
            });

            case "fetch_history_response" -> Platform.runLater(() -> {
                System.out.println("[SERVER] Handling fetch_history_response...");
                JsonObject payload = msg.getAsJsonObject("payload");
                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[ERROR] fetch_history_response status not ok.");
                    return;
                }

                chatHistory.clear();

                JsonArray arr = payload.getAsJsonArray("messages");
                if (arr != null) {
                    for (var m : arr) {
                        JsonObject o = m.getAsJsonObject();
                        int senderId = o.get("sender_id").getAsInt();
                        String senderName = o.get("sender_username").getAsString();
                        String avatarUrl = o.has("avatar_url") ? o.get("avatar_url").getAsString() : "";
                        String content = o.get("content").getAsString();
                        System.out.println("[HISTORY] Message from " + senderName + ": " + content);

                        chatHistory.add(new ChatMessage(senderId, senderName, avatarUrl, content));
                    }
                }

                renderMessages();
            });

            case "send_message" -> Platform.runLater(() -> {
                System.out.println("[SERVER] Handling incoming send_message...");
                JsonObject p = msg.getAsJsonObject("payload");

                int senderId = p.get("sender_id").getAsInt();
                String senderName = p.get("sender_username").getAsString();
                String avatarUrl = p.has("avatar_url") ? p.get("avatar_url").getAsString() : "";
                String content = p.get("content").getAsString();

                System.out.println("[INCOMING] Message from " + senderName + ": " + content);

                chatHistory.add(new ChatMessage(senderId, senderName, avatarUrl, content));
                renderMessages();
            });
            case "logout_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");
                if ("ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[LOGOUT] Server confirmed logout.");
                    switchToLogin();
                } else {
                    System.out.println("[LOGOUT] Logout failed: " + payload);
                    // Optionally show alert
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Logout failed on server.");
                    alert.show();
                }
            });

            default -> System.out.println("[SERVER] Unknown message type: " + type);
        }
    }

    /* ================= TRANSLATE ================= */
    @FXML
    private void toggleTranslate() {
        translateMode = !translateMode;
        System.out.println("[TRANSLATE] Translate mode: " + translateMode);
        if (translateMode) {
            translateButton.setText("Orignal");
            translateButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-background-radius: 6;");
        } else {
            translateButton.setText("Translate");
            translateButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-background-radius: 6;");
        }
        renderMessages();
    }

    /* ================= UI ================= */
    private void renderMessages() {
        System.out.println("[UI] Rendering messages. Total: " + chatHistory.size());
        messagesBox.getChildren().clear();

        int myId = Session.getUserId();

        for (ChatMessage m : chatHistory) {
            boolean mine = m.senderId() == myId;

            VBox messageContainer = new VBox(2); // small spacing between name and bubble

            if (!mine && activeChatIsGroup) {
                Label nameLabel = new Label(m.senderName());
                nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;"); // small & subtle
                messageContainer.getChildren().add(nameLabel);
            }

            Label msgLabel = new Label(m.content());
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(420);
            msgLabel.getStyleClass().add(mine ? "bubble-mine" : "bubble-other");

            messageContainer.getChildren().add(msgLabel); // ✅ add message AFTER name

            ImageView avatar = new ImageView(loadAvatar(m.avatarUrl()));
            avatar.setFitWidth(28);
            avatar.setFitHeight(28);
            avatar.setPreserveRatio(true);

            HBox row = new HBox(10);
            row.setStyle(mine ? "-fx-alignment: center-right;" : "-fx-alignment: center-left;");

            if (mine) row.getChildren().addAll(msgLabel, avatar); // mine: msg + avatar
            else row.getChildren().addAll(avatar, messageContainer); // others: avatar + VBox(name+msg)

            messagesBox.getChildren().add(row);

            // Translate asynchronously if needed
            if (!mine && translateMode) {
                String originalText = m.content();
                msgLabel.setText("translating...");

                new Thread(() -> {
                    try {
                        String translated = translationService.translate(
                                originalText,
                                Session.getPreferredLanguageCode()
                        );
                        Platform.runLater(() -> msgLabel.setText(translated));
                    } catch (Exception e) {
                        Platform.runLater(() -> msgLabel.setText(originalText)); // fallback
                        System.out.println("[TRANSLATE ERROR] " + e.getMessage());
                    }
                }).start();
            }

        }


        Platform.runLater(() -> messagesScroll.setVvalue(1));
    }


    private Parent settingsOverlay;

    private void openSettings() {
        try {
            if (settingsOverlay != null) return;

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/yegnachat/client/settings.fxml")
            );

            Parent overlay = loader.load();
            settingsOverlay = overlay;
            // Load CSS
            overlay.getStylesheets().add(
                    getClass().getResource("/css/settings.css").toExternalForm()
            );

            SettingsController controller = loader.getController();
            controller.setCloseCallback(this::closeSettings);
            controller.setLogoutCallback(this::handleLogout);
            controller.setSocket(socket);

            this.settingsController = controller;

            overlay.setPickOnBounds(false);
            rootStack.getChildren().add(overlay);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void closeSettings() {
        if (settingsOverlay != null) {
            rootStack.getChildren().remove(settingsOverlay);
            settingsOverlay = null;
            settingsController = null;
            socket.setOnMessage(this::handleServerMessage); // restore
        }
    }

    private void handleLogout() {
        System.out.println("[LOGOUT] Sending logout request to server...");

        // Build logout message
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "logout");
        msg.add("payload", new JsonObject());

        // Send to server
        socket.send(msg);

        // switch to login
        Platform.runLater(this::switchToLogin);
    }

    private void switchToLogin() {
        try {
            Session.clear();

            Stage oldStage = (Stage) rootStack.getScene().getWindow();
            oldStage.hide();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/yegnachat/client/login.fxml")
            );
            Scene scene = new Scene(loader.load());

            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.setTitle("Login");
            newStage.show();

            System.out.println("[LOGOUT] Fresh stage launched.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Parent chatInfoOverlay;

    private void openChatInfo(int userId, String username, String bio, String avatarUrl) {
        try {
            if (chatInfoOverlay != null) return;

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/yegnachat/client/chat_view.fxml")
            );
            Parent overlay = loader.load();
            ChatInfoController controller = loader.getController();

            controller.setUsername(username);
            controller.setBio(bio);
            controller.setAvatar(new ImageView(loadAvatar(avatarUrl)));
            controller.setCloseCallback(this::closeChatInfo);

            chatInfoOverlay = overlay;
            overlay.setPickOnBounds(false);
            rootStack.getChildren().add(overlay);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeChatInfo() {
        if (chatInfoOverlay != null) {
            rootStack.getChildren().remove(chatInfoOverlay);
            chatInfoOverlay = null;
        }
    }

    private Parent groupInfoOverlay;

    private void openGroupInfo(int groupId, String groupName, String groupAbout) {
        try {
            if (groupInfoOverlay != null) return;

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/yegnachat/client/group_view.fxml")
            );
            Parent overlay = loader.load();
            GroupInfoController controller = loader.getController();

            controller.setCloseCallback(this::closeGroupInfo);
            controller.groupNameLabel.setText(groupName);
            controller.groupAboutLabel.setText(groupAbout);
            // You can populate membersBox dynamically later

            groupInfoOverlay = overlay;
            overlay.setPickOnBounds(false);
            rootStack.getChildren().add(overlay);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeGroupInfo() {
        if (groupInfoOverlay != null) {
            rootStack.getChildren().remove(groupInfoOverlay);
            groupInfoOverlay = null;
        }
    }

}
