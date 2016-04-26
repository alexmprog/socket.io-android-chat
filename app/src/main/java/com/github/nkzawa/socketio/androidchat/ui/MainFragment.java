package com.github.nkzawa.socketio.androidchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.socketio.androidchat.R;
import com.github.nkzawa.socketio.androidchat.api.SocketIOEvent;
import com.github.nkzawa.socketio.androidchat.api.SocketIOManager;
import com.github.nkzawa.socketio.androidchat.api.response.BaseResponse;
import com.github.nkzawa.socketio.androidchat.api.response.MessageResponse;
import com.github.nkzawa.socketio.androidchat.api.response.UserLoggedResponse;
import com.github.nkzawa.socketio.androidchat.api.response.UserTypingResponse;
import com.github.nkzawa.socketio.androidchat.model.Message;

import java.util.ArrayList;
import java.util.List;


/**
 * A chat fragment containing messages view and input form.
 */
public class MainFragment extends Fragment {

    private static final int REQUEST_LOGIN = 0;

    private static final int TYPING_TIMER_LENGTH = 600;

    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;

    public MainFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        SocketIOManager manager = SocketIOManager.getInstance();

        manager.addListener(SocketIOEvent.SOCKET_EVENT_ERROR, onConnectError);
        manager.addListener(SocketIOEvent.SOCKET_EVENT_TIMEOUT, onConnectError);
        manager.addListener(SocketIOEvent.NEW_MESSAGE, onNewMessage);
        manager.addListener(SocketIOEvent.USER_JOINED, onUserJoined);
        manager.addListener(SocketIOEvent.USER_LEFT, onUserLeft);
        manager.addListener(SocketIOEvent.TYPING, onTyping);
        manager.addListener(SocketIOEvent.STOP_TYPING, onStopTyping);
        manager.connect();

        startSignIn();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SocketIOManager manager = SocketIOManager.getInstance();

        manager.disconnect();
        manager.removeListener(SocketIOEvent.SOCKET_EVENT_ERROR);
        manager.removeListener(SocketIOEvent.SOCKET_EVENT_TIMEOUT);
        manager.removeListener(SocketIOEvent.NEW_MESSAGE);
        manager.removeListener(SocketIOEvent.USER_JOINED);
        manager.removeListener(SocketIOEvent.USER_LEFT);
        manager.removeListener(SocketIOEvent.TYPING);
        manager.removeListener(SocketIOEvent.STOP_TYPING);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new MessageAdapter(getActivity(), mMessages);
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) view.findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                SocketIOManager manager = SocketIOManager.getInstance();

                if (!manager.isConnected()) return;

                if (!mTyping) {
                    mTyping = true;
                    manager.send(SocketIOEvent.TYPING);
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra(LoginActivity.LOGGED_USER_NAME);
        int numUsers = data.getIntExtra(LoginActivity.NUMBER_OF_USERS, 1);

        addLog(getResources().getString(R.string.message_welcome));
        addParticipantsLog(numUsers);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addLog(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addParticipantsLog(int numUsers) {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        mMessages.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() {
        if (null == mUsername) return;

        SocketIOManager manager = SocketIOManager.getInstance();

        if (!manager.isConnected()) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
        addMessage(mUsername, message);

        // perform the sending message attempt.
        manager.send(SocketIOEvent.NEW_MESSAGE, message);
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void leave() {
        mUsername = null;
        SocketIOManager manager = SocketIOManager.getInstance();
        manager.disconnect();
        manager.connect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private SocketIOManager.ResponseCallback<BaseResponse> onConnectError = new SocketIOManager.ResponseCallback<BaseResponse>() {
        @Override
        public void onResult(BaseResponse result) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public BaseResponse getResult() {
            return new BaseResponse();
        }
    };


    private SocketIOManager.ResponseCallback<MessageResponse> onNewMessage = new SocketIOManager.ResponseCallback<MessageResponse>() {
        @Override
        public void onResult(final MessageResponse result) {
           getActivity().runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   removeTyping(result.userName);
                   addMessage(result.userName, result.message);
               }
           });
        }

        @Override
        public MessageResponse getResult() {
            return new MessageResponse();
        }
    };

    private SocketIOManager.ResponseCallback<UserLoggedResponse> onUserJoined = new SocketIOManager.ResponseCallback<UserLoggedResponse>() {
        @Override
        public void onResult(final UserLoggedResponse result) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addLog(getResources().getString(R.string.message_user_joined, result.userName));
                    addParticipantsLog(result.numOfUsers);
                }
            });
        }

        @Override
        public UserLoggedResponse getResult() {
            return new UserLoggedResponse();
        }
    };

    private SocketIOManager.ResponseCallback<UserLoggedResponse> onUserLeft = new SocketIOManager.ResponseCallback<UserLoggedResponse>() {
        @Override
        public void onResult(final UserLoggedResponse result) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addLog(getResources().getString(R.string.message_user_left, result.userName));
                    addParticipantsLog(result.numOfUsers);
                    removeTyping(result.userName);
                }
            });
        }

        @Override
        public UserLoggedResponse getResult() {
            return new UserLoggedResponse();
        }
    };

    private SocketIOManager.ResponseCallback<UserTypingResponse> onTyping = new SocketIOManager.ResponseCallback<UserTypingResponse>() {
        @Override
        public void onResult(final UserTypingResponse result) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addTyping(result.userName);
                }
            });
        }

        @Override
        public UserTypingResponse getResult() {
            return new UserTypingResponse();
        }
    };

    private SocketIOManager.ResponseCallback<UserTypingResponse> onStopTyping = new SocketIOManager.ResponseCallback<UserTypingResponse>() {
        @Override
        public void onResult(final UserTypingResponse result) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    removeTyping(result.userName);
                }
            });
        }

        @Override
        public UserTypingResponse getResult() {
            return new UserTypingResponse();
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;

            SocketIOManager manager = SocketIOManager.getInstance();
            manager.send(SocketIOEvent.STOP_TYPING);
        }
    };
}

