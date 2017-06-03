package com.annalytics.rxmsg.democlient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.annalytics.rxmessenger.RxMessengerClient;
import com.annalytics.rxmsg.demo.model.RequestMessage;
import com.annalytics.rxmsg.demo.model.ResponseMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SERVER_ACTION = "com.annalytics.rxmsg.action.DEMO_SERVER";
    private static final String SERVER_PACKAGE = "com.annalytics.rxmsg.demoserver";
    private static final String SERVER_CLASS = "com.annalytics.rxmsg.demoserver.DemoService";

    @BindView(R.id.messageText)
    EditText messageText;

    @BindView(R.id.response_text)
    TextView response;

    private RxMessengerClient<RequestMessage, ResponseMessage> rxMessengerClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Intent intent = getServerIntent();
        rxMessengerClient = new RxMessengerClient<>(this, intent, ResponseMessage.class);
    }

    private Intent getServerIntent() {
        Intent intent = new Intent(SERVER_ACTION);
        intent.setClassName(SERVER_PACKAGE, SERVER_CLASS);
        return intent;
    }

    @OnClick(R.id.button_send)
    public void sendMessage() {
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setClientMessage(messageText.getText().toString());
        Log.d(TAG, "Sending message to server: " + messageText.getText().toString());
        rxMessengerClient.sendMessage(requestMessage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResponseMessage>() {
            @Override
            public void accept(@NonNull ResponseMessage responseMessage) throws Exception {
                Log.d(TAG, "Got message from server: " + responseMessage.getServerMessage());
                response.setText(responseMessage.getServerMessage());
            }
        });
    }
}
