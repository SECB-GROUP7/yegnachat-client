package com.yegnachat.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatClientSocket {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private Consumer<JsonObject> onMessage;
    private final List<Consumer<JsonObject>> listeners = new ArrayList<>();


    private static final Gson gson = new Gson();

    public ChatClientSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void startListening() {
        Thread.ofVirtual().start(() -> {
            try {
                String json;
                while ((json = reader.readLine()) != null) {
                    JsonObject msg = gson.fromJson(json, JsonObject.class);
                    if (onMessage != null) {
                        onMessage.accept(msg);
                    }
                }
            } catch (IOException e) {
                close();
            }
        });
    }

    public void setOnMessage(Consumer<JsonObject> handler) {
        this.onMessage = handler;
    }

    public void send(JsonObject message) {
        try {
            writer.write(gson.toJson(message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            close();
        }
    }
    public synchronized void sendRaw(InputStream in, long size) throws IOException {
        OutputStream out = socket.getOutputStream();
        byte[] buffer = new byte[8192];
        long remaining = size;

        while (remaining > 0) {
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read == -1) break;
            out.write(buffer, 0, read);
            remaining -= read;
        }
        out.flush();
    }

    public void addMessageListener(Consumer<JsonObject> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeMessageListener(Consumer<JsonObject> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void onRawMessage(JsonObject msg) {
        synchronized (listeners) {
            for (var listener : listeners) {
                listener.accept(msg);
            }
        }
    }


    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
