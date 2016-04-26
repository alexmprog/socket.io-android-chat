package com.github.nkzawa.socketio.androidchat.api;

import android.support.annotation.NonNull;

import com.github.nkzawa.socketio.androidchat.BuildConfig;
import com.github.nkzawa.socketio.androidchat.api.response.BaseResponse;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketIOManager {

    private static volatile SocketIOManager sInstance;

    public static SocketIOManager getInstance() {
        if (sInstance == null) {
            synchronized (SocketIOManager.class) {
                if (sInstance == null) {
                    sInstance = new SocketIOManager();
                }
            }
        }

        return sInstance;
    }

    private Socket mSocket;

    private SocketIOManager() {
        try {
            mSocket = IO.socket(BuildConfig.CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() {
        if (!mSocket.connected()) {
            mSocket.connect();
        }
    }

    public void disconnect() {
        if (mSocket.connected()) {
            mSocket.disconnect();
        }
    }

    public boolean isConnected() {
        return mSocket.connected();
    }

    public <T extends BaseResponse> void addListener(@NonNull SocketIOEvent event, @NonNull final ResponseCallback<T> callback) {
        mSocket.on(event.getEventName(), new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                T result = callback.getResult();
                callback.onResult((T) result.parseJson((JSONObject) args[0]));
            }
        });
    }

    public <T extends BaseResponse> void removeListener(@NonNull SocketIOEvent event) {
        mSocket.off(event.getEventName());
    }

    public void send(@NonNull SocketIOEvent event, @NonNull Object... args) {
        mSocket.emit(event.getEventName(), args);
    }

    public interface ResponseCallback<T extends BaseResponse> {

        void onResult(T result);

        T getResult();
    }
}
