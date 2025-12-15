package com.yegnachat.controllers;

import com.yegnachat.models.User;
import com.yegnachat.session.Session;
import com.yegnachat.util.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class ChatController {

    @FXML private ImageView avatarView;
    @FXML private Label usernameLabel;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button logoutButton;

    @FXML
    public void initialize() {
        User me = Session.getCurrentUser();
        if (me != null) {
            usernameLabel.setText(me.getUsername());
            if (me.getAvatarUrl() != null && !me.getAvatarUrl().isEmpty()) {
                try {
                    Image img = new Image(me.getAvatarUrl(), true);
                    avatarView.setImage(img);
                } catch (Exception e) {
                    // ignore and leave default
                }
            }
        } else {
            usernameLabel.setText("Unknown");
        }

        // UI hooks
        sendButton.setOnAction(e -> sendLocalMessage());
        messageField.setOnAction(e -> sendLocalMessage());

        logoutButton.setOnAction(e -> {
            try {
                Session.clear();
                Navigator.switchTo((Node) logoutButton, "login.fxml", "Login");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void sendLocalMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        // add message to UI immediately
        addMessageToBox("Me: " + text);
        messageField.clear();

        // TODO: send to server (Phase 2)
    }

    public void addMessageToBox(String message) {
        Platform.runLater(() -> {
            Label l = new Label(message);
            l.setWrapText(true);
            messagesBox.getChildren().add(l);
            // scroll to bottom
            messagesScroll.layout();
            messagesScroll.setVvalue(1.0);
        });
    }
}
