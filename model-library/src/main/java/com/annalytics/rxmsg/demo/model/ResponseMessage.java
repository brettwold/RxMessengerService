package com.annalytics.rxmsg.demo.model;


import com.annalytics.rxmessenger.BaseMessage;

public class ResponseMessage extends BaseMessage {

    private String serverMessage;

    public String getServerMessage() {
        return serverMessage;
    }

    public void setServerMessage(String serverMessage) {
        this.serverMessage = serverMessage;
    }

}
