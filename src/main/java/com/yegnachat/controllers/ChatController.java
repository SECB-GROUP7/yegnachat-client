package com.yegnachat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.yegnachat.util.TokenStorage;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.yegnachat.util.ImageUtil;

public class ChatController {
    @FXML
    public Button feedButton;
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
    @FXML
    private Button newChatButton;

    private SettingsController settingsController;

    // STATE ATTRIBUTES
    private ChatClientSocket socket;
    private Integer activeChatId = null;
    private boolean activeChatIsGroup = false;
    private Stage feedStage;

    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private TranslationService translationService;
    private boolean translateMode = false;
    private Integer pendingUserInfoRequest = null;
    private Integer pendingGroupInfoRequest = null;
    private VBox newChatResultsBox;
    private TextField newChatSearchField;


    private record ChatMessage(int senderId, String senderName, String avatarUrl, String content, String chatType,
                               int chatId) {
    }


    @FXML
    public void initialize() {
        System.out.println("[INIT] Initializing ChatController...");

        Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSansEthiopicVariable.ttf"), 14);

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
            if (activeChatIsGroup) {
                openGroupInfo(activeChatId);
                System.out.println("GroupID == " + activeChatId);
            } else {
                System.out.println("ChatID == " + activeChatId);

                openChatInfo(activeChatId);
            }
        });
        newChatButton.setOnAction(e -> openNewChatDialog());
        feedButton.setOnAction(e -> openFeedWindow());

    }

    private void setAvatar(String avatarUrl) {
        chatAvatar.setImage(ImageUtil.loadAvatar(avatarUrl));
    }

    // REQUEST LIST
    private void requestUserList() {
        System.out.println("[REQUEST] Sending list_users request...");
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "list_users");
        msg.add("payload", new JsonObject());
        socket.send(msg);
    }

    // OPEN CHAT
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
        payload.addProperty(key, id);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "fetch_history");
        msg.add("payload", payload);

        socket.send(msg);
    }

    private void routeIncomingMessage(ChatMessage msg) {

        // Ignore messages not belonging to active chat
        if (activeChatId == null) return;

        boolean sameChat =
                msg.chatType().equals(activeChatIsGroup ? "group" : "private")
                        && msg.chatId() == activeChatId;

        if (!sameChat) {
            System.out.println("[ROUTE] Message for inactive chat → ignored");
            return;
        }

        chatHistory.add(msg);
        renderMessages();
    }


    // Send logic
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

        if (activeChatIsGroup) payload.addProperty("group_id", String.valueOf(activeChatId));
        else payload.addProperty("receiver_id", String.valueOf(activeChatId));

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "send_message");
        msg.add("payload", payload);
        socket.send(msg);

        messageField.clear();
    }


    // Handle server message
    private void handleServerMessage(JsonObject msg) {
        if (!Session.isLoggedIn()) {
            System.out.println("[SERVER] Message ignored (not logged in)");
            return;
        }
        String type = msg.has("type") ? msg.get("type").getAsString() : "unknown";

        if (settingsController != null) {
            settingsController.handleServerMessage(type, msg.getAsJsonObject("payload"));
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

                        chatHistory.add(new ChatMessage(
                                senderId,
                                senderName,
                                avatarUrl,
                                content,
                                activeChatIsGroup ? "group" : "private",
                                activeChatId
                        ));

                    }
                }

                renderMessages();
            });

            case "send_message" -> Platform.runLater(() -> {
                JsonObject p = msg.getAsJsonObject("payload");

                String chatType = p.get("chat_type").getAsString();

                int senderId = p.has("sender_id") ? p.get("sender_id").getAsInt() : -1;
                int receiverId = p.has("receiver_id") ? p.get("receiver_id").getAsInt() : -1;

                int chatId;
                if ("group".equals(chatType)) {
                    chatId = p.has("group_id") ? p.get("group_id").getAsInt() : activeChatId;
                } else {
                    chatId = (senderId == Session.getUserId()) ? receiverId : senderId;
                }

                ChatMessage incoming = new ChatMessage(
                        senderId,
                        p.has("sender_username") ? p.get("sender_username").getAsString() : "Unknown",
                        p.has("avatar_url") ? p.get("avatar_url").getAsString() : "",
                        p.has("content") ? p.get("content").getAsString() : "",
                        chatType,
                        chatId
                );

                routeIncomingMessage(incoming);
                requestUserList();
            });


            case "logout_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");
                if ("ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[LOGOUT] Server confirmed logout.");
                    switchToLogin();
                } else {
                    System.out.println("[LOGOUT] Logout failed: " + payload);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Logout failed on server.");
                    alert.show();
                }
            });

            case "get_user_profile_response" -> Platform.runLater(() -> {
                if (pendingUserInfoRequest == null) return;

                JsonObject payload = msg.getAsJsonObject("payload");
                if (!"ok".equals(payload.get("status").getAsString())) return;

                JsonObject user = payload.getAsJsonObject("user");
                int userId = user.get("id").getAsInt();

                if (userId != pendingUserInfoRequest) return;
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/chat_view.fxml"));
                    Parent overlay = loader.load();
                    ChatInfoController controller = loader.getController();

                    controller.setBio(user.get("bio").getAsString());
                    controller.setUsername(user.get("username").getAsString());
                    controller.setAvatarUrl(
                            user.get("avatar_url").getAsString()
                    );
                    // Save controller for later use
                    overlay.getProperties().put("controller", controller);

                    controller.setCloseCallback(this::closeChatInfo);
                    chatInfoOverlay = overlay;
                    overlay.setPickOnBounds(false);
                    rootStack.getChildren().add(overlay);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pendingUserInfoRequest = null;
            });

            case "list_group_members_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");
                if (!payload.has("members")) return;

                JsonArray members = payload.getAsJsonArray("members");

                // If the overlay is open, populate members immediately
                if (groupInfoOverlay != null) {
                    Object controllerObj = groupInfoOverlay.getProperties().get("controller");
                    if (controllerObj instanceof GroupInfoController controller) {
                        controller.populateMembers(members);
                    }
                }

            });


            case "get_group_info_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");
                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[GROUP INFO] Error: " + payload.get("message").getAsString());
                    return;
                }


                JsonObject group = payload.getAsJsonObject("group");
                // request members AFTER opening UI
                JsonObject req = new JsonObject();
                req.addProperty("type", "list_group_members");
                JsonObject p = new JsonObject();
                p.addProperty("group_id", String.valueOf(group.get("id").getAsInt()));
                req.add("payload", p);

                socket.send(req);
                if (groupInfoOverlay != null) return; // already open

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/group_view.fxml"));
                    Parent overlay = loader.load();
                    GroupInfoController controller = loader.getController();

                    controller.groupNameLabel.setText(group.get("name").getAsString());
                    controller.groupAboutLabel.setText(group.get("about").getAsString());
                    controller.setGroupId(group.get("id").getAsInt());
                    controller.membersBox.getChildren().clear();

                    // Save controller for later use
                    overlay.getProperties().put("controller", controller);

                    controller.setCloseCallback(this::closeGroupInfo);
                    controller.addMemberButton.setOnAction(e -> openAddMemberDialog(group.get("id").getAsInt()));

                    groupInfoOverlay = overlay;
                    overlay.setPickOnBounds(false);
                    rootStack.getChildren().add(overlay);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });

            case "leave_group_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");

                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[GROUP] Leave failed: " + payload.get("message").getAsString());
                    return;
                }

                int leftGroupId = payload.get("group_id").getAsInt();

                // Close group info overlay
                closeGroupInfo();

                // If currently viewing this group → reset UI
                if (activeChatIsGroup && activeChatId != null && activeChatId == leftGroupId) {
                    activeChatId = null;
                    activeChatIsGroup = false;
                    chatTitle.setText("Select a chat");
                    chatSubtitle.setText("");
                    messagesBox.getChildren().clear();
                }

                // Refresh sidebar
                requestUserList();

                System.out.println("[GROUP] Successfully left group " + leftGroupId);
            });
            case "remove_user_from_group_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");

                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[GROUP] Kick failed");
                    return;
                }

                int removedUserId = payload.get("removed_user_id").getAsInt();
                System.out.println("[GROUP] User kicked: " + removedUserId);

                // Refresh members
                JsonObject req = new JsonObject();
                req.addProperty("type", "list_group_members");
                JsonObject p = new JsonObject();
                p.addProperty("group_id", String.valueOf(activeChatId));
                req.add("payload", p);

                socket.send(req);
            });

            case "promote_demote_user_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");

                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[GROUP] Role change failed: " + payload.get("message"));
                    return;
                }

                System.out.println("[GROUP] Role updated for user " + payload.get("user_id"));

                // Refresh group members
                JsonObject req = new JsonObject();
                req.addProperty("type", "list_group_members");

                JsonObject p = new JsonObject();
                p.addProperty("group_id", String.valueOf(activeChatId));

                req.add("payload", p);
                socket.send(req);
            });

            case "add_group_member_response" -> Platform.runLater(() -> {
                JsonObject payload = msg.getAsJsonObject("payload");

                if (!"ok".equals(payload.get("status").getAsString())) {
                    System.out.println("[GROUP] Add member failed: " + payload.get("message"));
                    return;
                }

                System.out.println("[GROUP] Member added successfully");

                // Refresh members
                JsonObject req = new JsonObject();
                req.addProperty("type", "list_group_members");

                JsonObject p = new JsonObject();
                p.addProperty("group_id", String.valueOf(activeChatId));

                req.add("payload", p);
                socket.send(req);
            });

            case "search_users_response" -> Platform.runLater(() -> {
                if (newChatResultsBox == null || newChatSearchField == null) return;

                newChatResultsBox.getChildren().clear();
                JsonArray users = msg.getAsJsonObject("payload").getAsJsonArray("users");
                if (users == null) return;

                for (var u : users) {
                    JsonObject user = u.getAsJsonObject();
                    Button userBtn = new Button(user.get("username").getAsString());
                    userBtn.setMaxWidth(Double.MAX_VALUE);
                    userBtn.getStyleClass().add("search-user-btn");

                    int userId = user.get("id").getAsInt();
                    userBtn.setOnAction(ev -> {
                        // Open private chat and send "hi"
                        openPrivate(userId, user.get("username").getAsString());
                        JsonObject hiMsg = new JsonObject();
                        hiMsg.addProperty("type", "send_message");
                        JsonObject hiPayload = new JsonObject();
                        hiPayload.addProperty("receiver_id", userId);
                        hiPayload.addProperty("content", "hi");
                        hiMsg.add("payload", hiPayload);
                        socket.send(hiMsg);

                        Stage stage = (Stage) newChatResultsBox.getScene().getWindow();
                        stage.close();
                    });
                    newChatResultsBox.getChildren().add(userBtn);
                }
            });


            case "error" -> Platform.runLater(() -> System.out.println("[SERVER ERROR] " + msg.get("payload")));

            default -> System.out.println("[SERVER] Unknown message type: " + type);
        }
    }

    // Translation
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

    // MANAGE UI
    private void renderMessages() {
        System.out.println("[UI] Rendering messages. Total: " + chatHistory.size());
        messagesBox.getChildren().clear();

        int myId = Session.getUserId();

        for (ChatMessage m : chatHistory) {
            boolean mine = m.senderId() == myId;

            VBox messageContainer = new VBox(2);

            if (!mine && activeChatIsGroup) {
                Label nameLabel = new Label(m.senderName());
                nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
                messageContainer.getChildren().add(nameLabel);
            }

            Label msgLabel = new Label(m.content());
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(420);
            msgLabel.getStyleClass().add(mine ? "bubble-mine" : "bubble-other");

            ImageView avatar = new ImageView(
                    ImageUtil.loadAvatar(m.avatarUrl())
            );
            // fitting width and height
            avatar.setFitWidth(28);
            avatar.setFitHeight(28);
            avatar.setPreserveRatio(true);

            HBox row = new HBox(10);
            row.setStyle(mine ? "-fx-alignment: center-right;" : "-fx-alignment: center-left;");

            if (mine) {
                // mine label goes directly into row
                row.getChildren().addAll(msgLabel, avatar);
            } else {
                // others label lives inside container
                messageContainer.getChildren().add(msgLabel);
                row.getChildren().addAll(avatar, messageContainer);
            }

            messagesBox.getChildren().add(row);

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
                        Platform.runLater(() -> msgLabel.setText(originalText));
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/settings.fxml"));

            Parent overlay = loader.load();
            settingsOverlay = overlay;
            // Load CSS
            overlay.getStylesheets().add(getClass().getResource("/css/settings.css").toExternalForm());

            SettingsController controller = loader.getController();
            controller.setCloseCallback(this::closeSettings);
            controller.setLogoutCallback(this::handleLogout);
            controller.setSocket(socket);

            this.settingsController = controller;

            overlay.setPickOnBounds(false);
            rootStack.getChildren().add(overlay);

        } catch (Exception e) {
            e.printStackTrace();
            socket.setOnMessage(this::handleServerMessage);
        }
    }


    private void closeSettings() {
        if (settingsOverlay != null) {
            rootStack.getChildren().remove(settingsOverlay);
            settingsOverlay = null;
            settingsController = null;
            socket.setOnMessage(this::handleServerMessage); // restore
        }
        requestUserList();
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
        TokenStorage.clearToken();
        Platform.runLater(this::switchToLogin);
    }

    private void switchToLogin() {
        try {
            Session.clear();

            Stage oldStage = (Stage) rootStack.getScene().getWindow();
            oldStage.hide();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/login.fxml"));
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

    private void openChatInfo(int userId) {
        pendingUserInfoRequest = userId;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "get_user_profile");

        JsonObject p = new JsonObject();
        p.addProperty("user_id", String.valueOf(userId));
        msg.add("payload", p);

        socket.send(msg);
    }


    private void closeChatInfo() {
        if (chatInfoOverlay != null) {
            rootStack.getChildren().remove(chatInfoOverlay);
            chatInfoOverlay = null;
        }
    }

    private Parent groupInfoOverlay;

    private void openGroupInfo(int groupId) {
        pendingGroupInfoRequest = groupId;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "get_group_info");
        JsonObject payload = new JsonObject();
        payload.addProperty("group_id", String.valueOf(groupId));
        msg.add("payload", payload);

        socket.send(msg);
    }


    private void closeGroupInfo() {
        if (groupInfoOverlay != null) {
            rootStack.getChildren().remove(groupInfoOverlay);
            groupInfoOverlay = null;
            pendingGroupInfoRequest = null;
        }
    }

    private void openAddMemberDialog(int groupId) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Enter username to add to the group");
        dialog.setContentText("Username:");

        dialog.showAndWait().ifPresent(username -> {
            if (username.isBlank()) return;

            JsonObject msg = new JsonObject();
            msg.addProperty("type", "add_group_member");

            JsonObject payload = new JsonObject();
            payload.addProperty("group_id", String.valueOf(groupId));
            payload.addProperty("username", username);

            msg.add("payload", payload);
            socket.send(msg);
        });
    }

    private void openNewChatDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Chat / Create Group");

        // --- Main container ---
        VBox container = new VBox(10);
        container.setId("container");

        // --- Tabs ---
        ToggleGroup toggleGroup = new ToggleGroup();
        ToggleButton newChatBtn = new ToggleButton("New Chat");
        ToggleButton newGroupBtn = new ToggleButton("Create Group");
        newChatBtn.setToggleGroup(toggleGroup);
        newGroupBtn.setToggleGroup(toggleGroup);
        newChatBtn.setSelected(true);

        HBox tabBox = new HBox(10, newChatBtn, newGroupBtn);
        container.getChildren().add(tabBox);

        // --- New Chat UI ---
        VBox newChatBox = new VBox(5);
        newChatSearchField = new TextField();
        newChatSearchField.setPromptText("Search users...");
        newChatResultsBox = new VBox(5);
        ScrollPane searchScroll = new ScrollPane(newChatResultsBox);
        searchScroll.setFitToWidth(true);
        searchScroll.setPrefHeight(200);
        newChatBox.getChildren().addAll(newChatSearchField, searchScroll);

        // --- Create Group UI ---
        VBox newGroupBox = new VBox(5);
        TextField groupNameField = new TextField();
        TextField aboutField = new TextField();
        groupNameField.setPromptText("Group name");
        aboutField.setPromptText("About");
        Button createGroupBtn = new Button("Create Group");
        createGroupBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-background-radius: 6;");
        newGroupBox.getChildren().addAll(groupNameField, aboutField, createGroupBtn);

        container.getChildren().add(newChatBox); // default

        // --- Tab switching ---
        toggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            container.getChildren().remove(1); // remove old content
            if (newT == newChatBtn) {
                container.getChildren().add(newChatBox);
            } else {
                container.getChildren().add(newGroupBox);
            }
        });

        // --- Dynamic search ---
        newChatSearchField.textProperty().addListener((obs, oldText, newText) -> {
            newChatResultsBox.getChildren().clear();
            if (!newText.isBlank()) {
                JsonObject searchMsg = new JsonObject();
                searchMsg.addProperty("type", "search_users");
                JsonObject payload = new JsonObject();
                payload.addProperty("query", newText);
                searchMsg.add("payload", payload);
                socket.send(searchMsg);
            }
        });

        // --- Create Group action ---
        createGroupBtn.setOnAction(ev -> {
            String name = groupNameField.getText().trim();
            String about = aboutField.getText().trim();
            if (!name.isEmpty()) {
                JsonObject createMsg = new JsonObject();
                createMsg.addProperty("type", "create_group");
                JsonObject payload = new JsonObject();
                payload.addProperty("name", name);
                payload.addProperty("about", about);
                createMsg.add("payload", payload);
                socket.send(createMsg);

                requestUserList();
                dialog.close();
            }
        });

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        scrollPane.getStylesheets().add(getClass().getResource("/css/newchat.css").toExternalForm());

        dialog.showAndWait();
    }

    private void openFeedWindow() {
        try {
            // Prevent opening multiple feed windows
            if (feedStage != null && feedStage.isShowing()) {
                feedStage.toFront();
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/yegnachat/client/feed.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root);

            feedStage = new Stage();
            feedStage.setTitle("Feed");
            feedStage.setScene(scene);


            feedStage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/feed.png"))
            );

            feedStage.setOnCloseRequest(e -> feedStage = null);

            feedStage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open feed").show();
        }
    }

}
