package com.annalytics.rxmessenger;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRxMessengerService<Q extends BaseMessage, P extends BaseMessage> extends Service {

    private static final String TAG = AbstractRxMessengerService.class.getSimpleName();

    protected static final int MESSAGE_REQUEST = 1;
    protected static final int MESSAGE_RESPONSE = 2;
    protected static final int MESSAGE_END_STREAM = 3;
    protected static final int MESSAGE_ERROR = 4;

    protected static final String KEY_REQUEST = "request";
    protected static final String KEY_RESPONSE = "response";

    protected static final String SENDER = "sender";

    private final Class<Q> requestType;

    private Map<String, Messenger> clientMap = new HashMap<>();

    static class IncomingHandler extends Handler {

        private final WeakReference<AbstractRxMessengerService> serviceRef;

        IncomingHandler(AbstractRxMessengerService service) {
            serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            AbstractRxMessengerService service = serviceRef.get();
            if (service != null) {
                switch (msg.what) {
                    case MESSAGE_REQUEST:
                        service.handleIncomingMessage(msg);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    protected final Messenger incomingMessenger = new Messenger(new AbstractRxMessengerService.IncomingHandler(this));

    protected AbstractRxMessengerService(Class<Q> requestType) {
        this.requestType = requestType;
    }

    private void handleIncomingMessage(Message msg) {
        Bundle data = msg.getData();
        if (data.containsKey(KEY_REQUEST)) {
            String requestJson = data.getString(KEY_REQUEST);
            try {
                Q object = BaseMessage.fromJson(requestJson, requestType);
                if (object != null) {
                    if (msg.replyTo != null) {
                        clientMap.put(object.getMessageId(), msg.replyTo);
                    }
                    String callingPackage = getCallingPackage(msg);
                    object.setSender(callingPackage);
                    handleRequest(object);
                } else {
                    Log.e(TAG, "Invalid message data: " + requestJson);
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid data sent to rxMessengerService", e);
            }
        }
    }

    private String getCallingPackage(Message msg) {
        String callingPackage = "";
        PackageManager pm = getPackageManager();
        if (pm != null) {
            String[] packages = pm.getPackagesForUid(msg.sendingUid);
            if (packages != null && packages.length >= 1) {
                callingPackage = packages[0];
            }
        }
        return callingPackage;
    }

    protected abstract void handleRequest(Q message);

    private Message createMessage(P sendMessage, String dataKey, int what, boolean withReply) {
        Bundle b = new Bundle();
        b.putString(dataKey, sendMessage.toJson());
        return createMessage(b, what, withReply);
    }

    private Message createMessage(Bundle b, int what, boolean withReply) {
        if (b == null) {
            b = new Bundle();
        }
        b.putString(SENDER, new ComponentName(getPackageName(), getClass().getName()).flattenToString());
        Message msg = Message.obtain(null, what);
        msg.setData(b);
        if (withReply) {
            msg.replyTo = incomingMessenger;
        }
        return msg;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return incomingMessenger.getBinder();
    }

    public void sendMessageToClient(String requestId, P response) {
        if (requestId != null && response != null) {
            sendMessage(requestId, createMessage(response, KEY_RESPONSE, MESSAGE_RESPONSE, false));
        }
    }

    public void sendEndStreamMessageToClient(String requestId) {
        if (requestId != null) {
            sendMessage(requestId, createMessage(null, MESSAGE_END_STREAM, false));
        }
    }

    public void sendErrorMessageToClient(String requestId, P response) {
        if (requestId != null) {
            sendMessage(requestId, createMessage(response, KEY_RESPONSE, MESSAGE_ERROR, false));
        }
    }

    private void sendMessage(String requestId, Message message) {
        Messenger target = clientMap.get(requestId);
        if (target != null) {
            try {
                target.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to send reply to client", e);
            }
        }
    }
}
