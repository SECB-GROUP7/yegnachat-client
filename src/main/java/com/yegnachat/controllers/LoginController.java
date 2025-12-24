package com.yegnachat.controllers;

import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.Socket;

public class LoginController {

    @FXML private BorderPane rootPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink goToSignup;
    @FXML private TextField passwordVisibleField;
    @FXML private CheckBox showPasswordCheck;

    private ChatClientSocket socket;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();
        try {
            socket = new ChatClientSocket(
                    new Socket(dotenv.get("HOST"), Integer.parseInt(dotenv.get("PORT")))
            );
            socket.startListening();
        } catch (Exception e) {
            showAlert("Error", "Cannot connect to server");
            return;
        }

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheck.selectedProperty().addListener((obs, o, isSelected) -> {
            passwordVisibleField.setVisible(isSelected);
            passwordVisibleField.setManaged(isSelected);
            passwordField.setVisible(!isSelected);
            passwordField.setManaged(!isSelected);
        });

        socket.setOnMessage(this::handleServerMessage);

        loginButton.setOnAction(e -> login());
        goToSignup.setOnAction(e -> switchTo("signup.fxml"));
    }

    private void login() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            showAlert("Error", "All fields required");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", usernameField.getText());
        payload.addProperty("password", passwordField.getText());

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "login");
        msg.add("payload", payload);

        socket.send(msg);
    }

    private void handleServerMessage(JsonObject msg) {
        if (!"login_response".equals(msg.get("type").getAsString())) return;

        JsonObject p = msg.getAsJsonObject("payload");

        Platform.runLater(() -> {
            if ("ok".equals(p.get("status").getAsString())) {
                Session.setAuth(
                        p.get("token").getAsString(),
                        p.get("user_id").getAsInt(),
                        p.get("preferred_language_code").getAsString()
                );
                Session.setSocket(socket);
                openChat();
            } else {
                showAlert("Error", "Invalid credentials");
            }
        });
    }

    private void openChat() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            StackPane root = FXMLLoader.load(
                    getClass().getResource("/com/yegnachat/client/chat.fxml")
            );

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/chat.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("YegnaChat");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void switchTo(String fxml) {
        try {
            rootPane.getScene().setRoot(
                    FXMLLoader.load(getClass().getResource("/com/yegnachat/client/" + fxml))
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
