package com.yegnachat.controllers;

import com.yegnachat.client.ChatClient;
import com.yegnachat.dao.UserDao;
import com.yegnachat.models.User;
import com.yegnachat.util.PasswordUtil;
import com.yegnachat.session.Session;
import com.yegnachat.util.Navigator;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.sql.*;

public class LoginController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private CheckBox showPasswordCheck;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink goToSignup;

    private String DB_URL;
    private String DB_USER;
    private String DB_PASS;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();

        DB_URL = "jdbc:mysql://" + dotenv.get("DB_HOST") + ":" + dotenv.get("DB_PORT") + "/" + dotenv.get("DB_NAME");
        DB_USER = dotenv.get("DB_USER");
        DB_PASS = dotenv.get("DB_PASS");

        // Show/hide password
        showPasswordCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passwordVisibleField.setText(passwordField.getText());
                passwordVisibleField.setVisible(true);
                passwordVisibleField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
            } else {
                passwordField.setText(passwordVisibleField.getText());
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordVisibleField.setVisible(false);
                passwordVisibleField.setManaged(false);
            }
        });

        goToSignup.setOnAction(e -> switchTo("signup.fxml"));
        loginButton.setOnAction(e -> login());
    }

    private void login() {
        String username = usernameField.getText();
        String password = showPasswordCheck.isSelected()
                ? passwordVisibleField.getText()
                : passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showAlert("Error", "All fields required");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            UserDao userDao = new UserDao(conn);

            User user = userDao.getUserByUsername(username);

            if (user == null) {
                showAlert("Error", "Invalid username or password");
                return;
            }


            if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) { // Replace with hashing ASAP
                showAlert("Error", "Invalid username or password");
                return;
            }

            showAlert("Success", "Logged in!");
            Session.setCurrentUser(user);
            Navigator.switchTo(loginButton, "chat.fxml", "Chat");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", "Database error");
        }
    }


    private void switchTo(String fxml) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(FXMLLoader.load(ChatClient.class.getResource(fxml)));
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
