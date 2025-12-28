package com.yegnachat.controllers;

import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.yegnachat.util.ImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class SettingsController {

    private ChatClientSocket socket;
    private Runnable closeCallback;
    @FXML
    private Button closeButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button editBioButton;
    @FXML
    private Button changePasswordButton;
    @FXML
    private Button saveLanguageButton;

    @FXML
    private Label usernameLabel;
    @FXML
    private Label bioLabel;
    @FXML
    private TextField languageField;
    @FXML
    private ImageView avatarImage;
    private Runnable logoutCallback;


    public void setLogoutCallback(Runnable logoutCallback) {
        this.logoutCallback = logoutCallback;
    }


    @FXML
    public void initialize() {

        closeButton.setOnAction(e -> {
            if (closeCallback != null) closeCallback.run();
        });

        editBioButton.setOnAction(e -> {
            if (socket != null) openEditBioDialog();
        });

        changePasswordButton.setOnAction(e -> {
            if (socket != null) openChangePasswordDialog();
        });

        saveLanguageButton.setOnAction(e -> {
            if (socket != null) savePreferredLanguage();
        });

        logoutButton.setOnAction(e -> logout());
    }

    public void setSocket(ChatClientSocket socket) {
        this.socket = socket;
        requestInitialSettings();
    }

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    // SOCKET

    public void handleServerMessage(String type, JsonObject payload) {

        switch (type) {

            case "get_user_response" -> {
                if ("ok".equals(payload.get("status").getAsString())) {
                    JsonObject user = payload.getAsJsonObject("user");

                    Platform.runLater(() -> {
                        usernameLabel.setText(user.get("username").getAsString());
                        bioLabel.setText(user.get("bio").getAsString());

                        String avatarUrl = user.get("avatar_url").getAsString();
                        System.out.println("Avatar_URL: "+avatarUrl);
                        setAvatar(avatarUrl);
                    });
                }
            }


            case "get_preferred_language_response" -> {
                if ("ok".equals(payload.get("status").getAsString())) {
                    Platform.runLater(() -> {
                        languageField.setText(
                                payload.get("preferred_language_code").getAsString()
                        );
                    });

                }
            }

            case "set_preferred_language_response" -> {
                if ("ok".equals(payload.get("status").getAsString())) {
                    Platform.runLater(() -> {
                        String code = payload.get("preferred_language_code").getAsString();
                        languageField.setText(code);
                        Session.setPreferredLanguageCode(code);
                    });

                }
            }

            case "set_password_response" -> {
                Platform.runLater(() -> {
                    showInfo(
                            "ok".equals(payload.get("status").getAsString())
                                    ? "Success"
                                    : "Error",
                            "Password update result received"
                    );
                });

            }

        }
    }

    private void requestInitialSettings() {
        send("get_user");
        send("get_preferred_language");
    }

    //Sending

    private void send(String type) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        msg.add("payload", new JsonObject());
        socket.send(msg);
    }

    private void send(String type, JsonObject payload) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        msg.add("payload", payload);
        socket.send(msg);
    }

    private void openEditBioDialog() {
        TextInputDialog dialog = new TextInputDialog(bioLabel.getText());
        dialog.setTitle("Edit Bio");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(bio -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("bio", bio);
            send("set_bio", payload);
            bioLabel.setText(bio);
        });
    }

    private void openChangePasswordDialog() {
        Platform.runLater(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Change Password");

            PasswordField oldPass = new PasswordField();
            oldPass.setPromptText("Old Password");
            PasswordField newPass = new PasswordField();
            newPass.setPromptText("New Password");

            VBox box = new VBox(10, oldPass, newPass);
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    JsonObject payload = new JsonObject();
                    payload.addProperty("old_password", oldPass.getText());
                    payload.addProperty("new_password", newPass.getText());
                    send("set_password", payload);
                }
            });
        });
    }

    private void logout() {
        if (socket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "logout");
            msg.add("payload", new JsonObject());
            socket.send(msg);
        }

        if (logoutCallback != null) {
            logoutCallback.run();
        }
    }


    private void savePreferredLanguage() {
        String code = languageField.getText().trim();
        if (code.isEmpty()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("language_code", code);
        send("set_preferred_language", payload);
    }

    private void showInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(title);
            alert.setContentText(msg);
            alert.show();
        });
    }

    private void setAvatar(String avatarUrl) {
        Image img = ImageUtil.loadAvatar(avatarUrl);
        avatarImage.setImage(img);
    }


    @FXML
    private void onChangeAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Avatar Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.png", "*.jpg", "*.jpeg", "*.webp"
                )
        );

        File file = chooser.showOpenDialog(
                avatarImage.getScene().getWindow()
        );

        if (file == null) return;

        new Thread(() -> {
            try {
                String avatarUrl = ImageUtil.uploadAvatar(file, Session.getUserId());
                Platform.runLater(() -> avatarImage.setImage(ImageUtil.loadAvatar(avatarUrl)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();



    }


}
