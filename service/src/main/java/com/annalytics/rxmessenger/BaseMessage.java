package com.annalytics.rxmessenger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.util.UUID;

public class BaseMessage {

    private static final Gson GSON = new GsonBuilder().create();

    private String messageId;
    private String sender;
    private String error;

    public BaseMessage() {
        messageId = UUID.randomUUID().toString();
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean isError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static <T extends BaseMessage> T fromJson(String json, Class<T> type) throws JsonParseException {
        return type.cast(GSON.fromJson(json, type));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseMessage that = (BaseMessage) o;

        return messageId != null ? messageId.equals(that.messageId) : that.messageId == null;

    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}
