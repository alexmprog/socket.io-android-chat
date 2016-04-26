package com.github.nkzawa.socketio.androidchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.socketio.androidchat.R;
import com.github.nkzawa.socketio.androidchat.api.SocketIOEvent;
import com.github.nkzawa.socketio.androidchat.api.SocketIOManager;
import com.github.nkzawa.socketio.androidchat.api.response.UserLoggedResponse;

/**
 * A login screen that offers login via username.
 */
public class LoginActivity extends Activity {

    public static final String NUMBER_OF_USERS = "numUsers";
    public static final String LOGGED_USER_NAME = "username";

    private EditText mUsernameView;

    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username_input);
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        SocketIOManager.getInstance().addListener(SocketIOEvent.LOGIN, onLogin);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SocketIOManager.getInstance().removeListener(SocketIOEvent.LOGIN);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        }

        mUsername = username;

        // perform the user login attempt.
        SocketIOManager.getInstance().send(SocketIOEvent.ADD_USER, username);
    }

    private SocketIOManager.ResponseCallback onLogin = new SocketIOManager.ResponseCallback<UserLoggedResponse>() {
        @Override
        public void onResult(UserLoggedResponse result) {
            Intent intent = new Intent();
            intent.putExtra(LOGGED_USER_NAME, mUsername);
            intent.putExtra(NUMBER_OF_USERS, result.numOfUsers);
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public UserLoggedResponse getResult() {
            return new UserLoggedResponse();
        }
    };
}



