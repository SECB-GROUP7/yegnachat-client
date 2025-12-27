package com.yegnachat.controllers;

import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.InputStream;

public class GroupInfoController {

    @FXML
    private VBox root;

    @FXML
    private Button closeButton;

    @FXML
    private ImageView groupAvatar;

    @FXML
    protected Label groupNameLabel;

    @FXML
    protected Label groupAboutLabel;

    @FXML
    protected VBox membersBox;

    @FXML
    protected Button addMemberButton;

    @FXML
    private Button leaveGroupButton;

    private Runnable closeCallback;
    private int groupId;
    private ChatClientSocket socket;

    @FXML
    public void initialize() {
        root.getStylesheets().add(
                getClass().getResource("/css/group-info.css").toExternalForm()
        );
        socket = Session.getSocket();
        closeButton.setOnAction(e -> {
            if (closeCallback != null) closeCallback.run();
        });

        leaveGroupButton.setOnAction(e -> leaveGroup());
    }

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    public void setSocket(ChatClientSocket socket) {
        this.socket = socket;
    }

    public void setGroupId(int id) {
        this.groupId = id;
    }

    public VBox getMembersBox() {
        return membersBox;
    }

    public void populateMembers(JsonArray members) {
        membersBox.getChildren().clear();

        int myId = Session.getUserId();

        String myRole = getMyRole(members);
        boolean iAmAdmin = myRole.equals("admin") || myRole.equals("owner");
        boolean iAmOwner = myRole.equals("owner");

        for (var m : members) {
            JsonObject member = m.getAsJsonObject();
            int userId = member.get("id").getAsInt();
            String username = member.get("username").getAsString();
            String role = member.get("role").getAsString();
            String avatarUrl = member.has("avatar_url") ? member.get("avatar_url").getAsString() : null;

            HBox row = new HBox(10);
            row.setStyle("-fx-alignment: center-left;");

            ImageView avatar = new ImageView(loadAvatar(avatarUrl));
            avatar.setFitWidth(32);
            avatar.setFitHeight(32);
            avatar.setPreserveRatio(true);

            VBox textBox = new VBox(2);
            Label nameLabel = new Label(username);
            nameLabel.setStyle("-fx-font-weight: bold;");
            Label roleLabel = new Label("(" + role + ")");
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
            textBox.getChildren().addAll(nameLabel, roleLabel);

            row.getChildren().addAll(avatar, textBox);

            // KICK BUTTON
            if (
                    iAmAdmin &&
                            !role.equals("owner") &&
                            userId != myId
            ) {
                Button kickBtn = new Button("Kick");
                kickBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
                kickBtn.setOnAction(e -> kickUser(userId));
                row.getChildren().add(kickBtn);
            }
            // PROMOTE / DEMOTE BUTTON (OWNER ONLY)
            if (
                    iAmOwner &&
                            !role.equals("owner") &&
                            userId != myId
            ) {
                Button promoteBtn = new Button(
                        role.equals("admin") ? "Demote" : "Promote"
                );

                promoteBtn.setStyle(
                        role.equals("admin")
                                ? "-fx-background-color: #f0ad4e; -fx-text-fill: white;"
                                : "-fx-background-color: #5bc0de; -fx-text-fill: white;"
                );

                promoteBtn.setOnAction(e -> {
                    String newRole = role.equals("admin") ? "member" : "admin";
                    changeUserRole(userId, newRole);
                });

                row.getChildren().add(promoteBtn);
            }

            membersBox.getChildren().add(row);
        }
    }

    private boolean isCurrentUserAdmin(JsonArray members) {
        int myId = Session.getUserId();
        for (var m : members) {
            JsonObject o = m.getAsJsonObject();
            if (o.get("id").getAsInt() == myId) {
                String role = o.get("role").getAsString();
                return role.equals("admin") || role.equals("owner");
            }
        }
        return false;
    }

    private void kickUser(int userId) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "remove_user_from_group");

        JsonObject payload = new JsonObject();
        payload.addProperty("group_id", String.valueOf(groupId));
        payload.addProperty("user_id", String.valueOf(userId));

        msg.add("payload", payload);
        socket.send(msg);
    }


    private Image loadAvatar(String url) {
        InputStream is;
        if (url == null || url.isBlank()) {
            is = getClass().getResourceAsStream("/icons/user.png");
        } else {
            is = getClass().getResourceAsStream(url);
            if (is == null) is = getClass().getResourceAsStream("/icons/user.png");
        }
        return new Image(is);
    }

    private String getMyRole(JsonArray members) {
        int myId = Session.getUserId();
        for (var m : members) {
            JsonObject o = m.getAsJsonObject();
            if (o.get("id").getAsInt() == myId) {
                return o.get("role").getAsString();
            }
        }
        return "member";
    }

    private void changeUserRole(int userId, String newRole) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "promote_demote_user");

        JsonObject payload = new JsonObject();
        payload.addProperty("group_id", String.valueOf(groupId));
        payload.addProperty("user_id", String.valueOf(userId));
        payload.addProperty("new_role", newRole);

        msg.add("payload", payload);
        socket.send(msg);
    }


    private void leaveGroup() {
        if (socket == null) return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "leave_group");
        JsonObject payload = new JsonObject();
        payload.addProperty("group_id", String.valueOf(groupId));
        msg.add("payload", payload);

        socket.send(msg);
    }
}
