package com.yegnachat.controllers;

import com.yegnachat.util.ImageUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class ChatInfoController {

    @FXML
    private VBox root;

    @FXML
    private Button closeButton;

    @FXML
    private ImageView avatarImage;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label bioLabel;

    private Runnable closeCallback;

    @FXML
    public void initialize() {
        // Attach CSS here (controller owns styling)
        root.getStylesheets().add(
                getClass().getResource("/css/chat-info.css").toExternalForm()
        );

        closeButton.setOnAction(e -> {
            if (closeCallback != null) {
                closeCallback.run();
            }
        });
    }

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    public void setBio(String bio) {
        bioLabel.setText(bio);
    }

    public void setAvatarUrl(String avatarUrl) {
        avatarImage.setImage(
                ImageUtil.loadAvatar(avatarUrl)
        );
    }
}
