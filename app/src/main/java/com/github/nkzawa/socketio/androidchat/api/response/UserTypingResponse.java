package com.github.nkzawa.socketio.androidchat.api.response;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class UserTypingResponse extends BaseResponse {

    public static final String USERNAME = "username";

    public String userName;

    public int numOfUsers;

    @Override
    public UserTypingResponse parseJson(@NonNull JSONObject data) {
        try {
            userName = data.getString(USERNAME);
        } catch (JSONException e) {
            return this;
        }

        return this;
    }

}
