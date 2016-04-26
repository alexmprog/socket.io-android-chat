package com.github.nkzawa.socketio.androidchat.api.response;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageResponse extends BaseResponse{

    public static final String MESSAGE = "message";
    public static final String USERNAME = "username";

    public String userName;

    public String message;

    @Override
    public MessageResponse parseJson(@NonNull JSONObject data){
        try {
            userName = data.getString(MESSAGE);
            message = data.getString(USERNAME);
        } catch (JSONException e) {
            return this;
        }

        return this;
    }
}
