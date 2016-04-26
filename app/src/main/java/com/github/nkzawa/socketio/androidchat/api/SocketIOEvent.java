package com.github.nkzawa.socketio.androidchat.api;

import android.support.annotation.NonNull;

import io.socket.client.Socket;

public enum SocketIOEvent {

    LOGIN("login"),
    ADD_USER("add user"),
    SOCKET_EVENT_ERROR(Socket.EVENT_CONNECT_ERROR),
    SOCKET_EVENT_TIMEOUT(Socket.EVENT_CONNECT_TIMEOUT),
    NEW_MESSAGE("new message"),
    USER_JOINED("user joined"),
    USER_LEFT("user left"),
    TYPING("typing"),
    STOP_TYPING("stop typing");



    private String eventName;

    SocketIOEvent(@NonNull String value) {
        this.eventName = value;
    }

    public String getEventName() {
        return eventName;
    }
}
