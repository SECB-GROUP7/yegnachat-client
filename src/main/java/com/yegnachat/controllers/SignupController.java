package com.yegnachat.controllers;

import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.net.Socket;

public class SignupController {

    @FXML private BorderPane rootPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Hyperlink goToLogin;

    private ChatClientSocket socket;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();
        try {
            socket = new ChatClientSocket(new Socket(dotenv.get("HOST"),Integer.parseInt(dotenv.get("PORT"))));
            socket.startListening();
        } catch (Exception e) {
            showAlert("Error", "Cannot connect to server");
            return;
        }

        socket.setOnMessage(this::handleServerMessage);

        signupButton.setOnAction(e -> signup());
        goToLogin.setOnAction(e -> switchToLogin());
    }

    private void signup() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        String c = confirmPasswordField.getText();

        if (u.isBlank() || p.isBlank() || c.isBlank()) {
            showAlert("Error", "All fields required");
            return;
        }

        if (!p.equals(c)) {
            showAlert("Error", "Passwords do not match");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", u);
        payload.addProperty("password", p);
        payload.addProperty("avatar_url", "");
        payload.addProperty("bio", "");

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "signup");
        msg.add("payload", payload);

        socket.send(msg);
    }

    private void handleServerMessage(JsonObject msg) {
        if (!"signup_response".equals(msg.get("type").getAsString())) return;

        Platform.runLater(() -> {
            showAlert("Success", "Account created! Please login.");
            switchToLogin();
        });
    }

    private void switchToLogin() {
        try {
            rootPane.getScene().setRoot(
                    javafx.fxml.FXMLLoader.load(
                            getClass().getResource("/com/yegnachat/client/login.fxml")
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.show();
    }
}
