package com.yegnachat.server;

import com.yegnachat.controllers.ChatController;
import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private ChatController chatController;

    public Client(String username, Socket socket, ChatController chatController) {
        try {
            this.socket = socket;
            this.username = username;
            this.chatController = chatController;

            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send username first
            writer.write(username);
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            closeEverything();
        }
    }

    public void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void listenForMessages() {
        Thread listener = new Thread(() -> {
            String msg;
            try {
                while ((msg = reader.readLine()) != null) {
                    // Send message to ChatController for UI update
                    chatController.addMessageToBox(msg);
                }
            } catch (IOException e) {
                closeEverything();
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private void closeEverything() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
