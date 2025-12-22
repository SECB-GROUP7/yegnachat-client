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
import javafx.stage.Stage;

import java.net.Socket;

public class LoginController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink goToSignup;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private CheckBox showPasswordCheck;

    private ChatClientSocket socket;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();
        try {
            socket = new ChatClientSocket(new Socket(dotenv.get("HOST"), Integer.parseInt(dotenv.get("PORT"))));
            socket.startListening();
        } catch (Exception e) {
            showAlert("Error", "Cannot connect to server");
            return;
        }
        // SHOW/HIDE password
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
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
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showAlert("Error", "All fields required");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "login");
        msg.add("payload", payload);

        socket.send(msg);
    }

    private void handleServerMessage(JsonObject msg) {
        if (!"login_response".equals(msg.get("type").getAsString())) return;

        JsonObject payload = msg.getAsJsonObject("payload");
        String status = payload.get("status").getAsString();

        Platform.runLater(() -> {
            if ("ok".equals(status)) {
                Session.setAuth(
                        payload.get("token").getAsString(),
                        payload.get("user_id").getAsInt()
                );
                Session.setSocket(socket);
                openChat();
            } else {
                showAlert("Error", "Invalid username or password");
            }
        });
    }

    private void openChat() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/com/yegnachat/client/chat.fxml"));

            BorderPane root = loader.load();

            // Load the scene
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
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(FXMLLoader.load(
                    getClass().getResource("/com/yegnachat/client/" + fxml)
            ));
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
