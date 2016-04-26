package com.github.nkzawa.socketio.androidchat.api.response;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class UserLoggedResponse extends BaseResponse {

    public static final String USERNAME = "username";
    public static final String NUM_OF_USERS = "numUsers";

    public String userName;

    public int numOfUsers;

    @Override
    public UserLoggedResponse parseJson(@NonNull JSONObject data) {
        try {
            userName = data.getString(USERNAME);
            numOfUsers = data.getInt(NUM_OF_USERS);
        } catch (JSONException e) {
            return this;
        }

        return this;
    }

}
