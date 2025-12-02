package com.yegnachat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    private static final ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // First message from client = username
        this.username = bufferedReader.readLine();
        broadcastMessage("Server: " + username + " has joined the chat");
    }

    public static void addClientHandler(ClientHandler handler) {
        synchronized (clientHandlers) {
            clientHandlers.add(handler);
        }
    }

    @Override
    public void run() {
        String message;

        try {
            while ((message = bufferedReader.readLine()) != null) {
                broadcastMessage(username + ": " + message);
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void broadcastMessage(String message) {
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (clientHandler != this) {
                        clientHandler.bufferedWriter.write(message);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    clientHandler.closeEverything();
                }
            }
        }
        System.out.println(message); // server console log
    }

    private void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();

            synchronized (clientHandlers) {
                clientHandlers.remove(this);
            }

            broadcastMessage("Server: " + username + " has left the chat");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
