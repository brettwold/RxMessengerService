package com.annalytics.rxmsg.demo.model;


import com.annalytics.rxmessenger.BaseMessage;

public class RequestMessage extends BaseMessage {

    private String clientMessage;

    public String getClientMessage() {
        return clientMessage;
    }

    public void setClientMessage(String clientMessage) {
        this.clientMessage = clientMessage;
    }
}
