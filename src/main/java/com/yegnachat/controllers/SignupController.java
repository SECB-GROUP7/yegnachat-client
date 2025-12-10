package com.yegnachat.controllers;

import com.yegnachat.dao.UserDao;
import com.yegnachat.models.User;

import com.yegnachat.server.PasswordUtil;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;

public class SignupController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;

    private UserDao userDao;

    @FXML
    public void initialize() {
        Dotenv dotenv = Dotenv.load();
        String dbUrl = "jdbc:mysql://" + dotenv.get("DB_HOST") + ":" + dotenv.get("DB_PORT") + "/" + dotenv.get("DB_NAME");
        String dbUser = dotenv.get("DB_USER");
        String dbPass = dotenv.get("DB_PASS");

        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            userDao = new UserDao(conn);

        } catch (Exception e) {
            statusLabel.setText("DB error: " + e.getMessage());
        }
    }

    @FXML
    public void handleSignup() {
        try {
            User u = new User();
            u.setUsername(usernameField.getText().toLowerCase());
            u.setPasswordHash(PasswordUtil.hashPassword(passwordField.getText()));
            u.setAvatarUrl(null);
            u.setBio("");

            boolean success = userDao.createUser(u);

            if (success) statusLabel.setText("Account created!");
            else statusLabel.setText("Signup failed!");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
