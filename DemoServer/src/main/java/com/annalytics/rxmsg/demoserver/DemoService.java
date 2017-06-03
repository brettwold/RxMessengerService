package com.annalytics.rxmsg.demoserver;


import android.util.Log;

import com.annalytics.rxmessenger.AbstractRxMessengerService;
import com.annalytics.rxmsg.demo.model.RequestMessage;
import com.annalytics.rxmsg.demo.model.ResponseMessage;

public class DemoService extends AbstractRxMessengerService<RequestMessage, ResponseMessage> {

    private static final String TAG = DemoService.class.getSimpleName();

    public DemoService() {
        super(RequestMessage.class);
    }

    @Override
    protected void handleRequest(RequestMessage requestMessage) {
        Log.d(TAG, String.format("Message from client %s. Replying", requestMessage.getClientMessage()));

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setServerMessage(String.format("Hello %s from the rxMessenger server", requestMessage.getClientMessage()));
        sendMessageToClient(requestMessage.getMessageId(), responseMessage);
        sendEndStreamMessageToClient(requestMessage.getMessageId());
    }
}
