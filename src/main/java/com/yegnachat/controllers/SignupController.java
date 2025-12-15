package com.yegnachat.controllers;

import com.yegnachat.client.ChatClient;
import com.yegnachat.dao.UserDao;
import com.yegnachat.models.User;
import com.yegnachat.server.PasswordUtil;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Locale;

public class SignupController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button signupButton;
    @FXML
    private Hyperlink goToLogin;

    private String DB_URL;
    private String DB_USER;
    private String DB_PASS;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();
        DB_URL = "jdbc:mysql://" + dotenv.get("DB_HOST") + ":" + dotenv.get("DB_PORT") + "/" + dotenv.get("DB_NAME");
        DB_USER = dotenv.get("DB_USER");
        DB_PASS = dotenv.get("DB_PASS");

        signupButton.setOnAction(e -> signup());
        goToLogin.setOnAction(e -> switchTo("login.fxml"));
    }

    private void signup() {
        String username = usernameField.getText();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isBlank() || pass.isBlank() || confirm.isBlank()) {
            showAlert("Error", "All fields required");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert("Error", "Passwords do not match");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            UserDao userDao = new UserDao(conn);

            User user = new User();
            user.setUsername(username.replaceAll("\\s+", "").toLowerCase());
            user.setPasswordHash(PasswordUtil.hashPassword(pass));
            user.setBio(null);
            user.setAvatarUrl(null);

            if (userDao.createUser(user)) {
                showAlert("Success", "Account created! You can log in now.");
                switchTo("login.fxml");
            } else {
                showAlert("Error", "Error creating Account.");
            }
            
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
