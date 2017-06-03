package com.annalytics.rxmessenger;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

import static com.annalytics.rxmessenger.AbstractRxMessengerService.SENDER;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.KEY_REQUEST;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.KEY_RESPONSE;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_END_STREAM;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_ERROR;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_REQUEST;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_RESPONSE;

public class RxMessengerClient<Q extends BaseMessage, P extends BaseMessage> {

    private static final String TAG = RxMessengerClient.class.getSimpleName();

    private final Context context;
    private final Intent serviceIntent;
    private final Class<P> responseType;

    protected static class MessengerConnection<Q extends BaseMessage, P extends BaseMessage> implements ServiceConnection {

        final RxMessengerClient baseMessengerClient;
        Messenger outgoingMessenger;
        ComponentName componentName;
        boolean bound = false;
        Class<P> responseType;
        BehaviorSubject<MessengerConnection<Q, P>> bindSubject = BehaviorSubject.create();

        MessengerConnection(RxMessengerClient baseMessengerClient, Class<P> responseType) {
            this.baseMessengerClient = baseMessengerClient;
            this.responseType = responseType;
        }

        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            if (componentName != null) {
                Log.d(RxMessengerClient.class.getSimpleName(), "Bound to service - " + componentName.flattenToString());
            }
            this.componentName = componentName;
            outgoingMessenger = new Messenger(binder);
            bound = true;
            bindSubject.onNext(this);
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className != null) {
                Log.d(RxMessengerClient.class.getSimpleName(), "Unbound from service - " + className.flattenToString());
            }
            bound = false;
            bindSubject.onComplete();
        }

        Observable<MessengerConnection<Q, P>> getConnectedObservable() {
            return bindSubject;
        }

        boolean isBound() {
            return bound;
        }

        void sendMessage(Q request, ObservableEmitter callbackEmitter) {
            if (request != null) {
                Message msg = Message.obtain(null, MESSAGE_REQUEST);
                Bundle data = new Bundle();
                data.putString(KEY_REQUEST, request.toJson());
                data.putString(SENDER, componentName.flattenToString());
                msg.setData(data);
                msg.replyTo = new Messenger(new IncomingHandler(baseMessengerClient, callbackEmitter, responseType));
                try {
                    outgoingMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to send message", e);
                }
            }
        }

        void shutDown() {
            baseMessengerClient.context.unbindService(this);
            bound = false;
        }
    }

    private static class IncomingHandler<P extends BaseMessage> extends Handler {

        private final WeakReference<RxMessengerClient> serviceRef;
        private ObservableEmitter<P> callbackEmitter;
        private Class<P> responseType;

        IncomingHandler(RxMessengerClient service, ObservableEmitter<P> callbackEmitter, Class<P> responseType) {
            serviceRef = new WeakReference<>(service);
            this.callbackEmitter = callbackEmitter;
            this.responseType = responseType;
        }

        @Override
        public void handleMessage(Message msg) {
            RxMessengerClient client = serviceRef.get();
            if (client != null) {
                Bundle data = msg.getData();
                if (data != null) {
                    String sender = data.getString(SENDER);
                    switch (msg.what) {
                        case MESSAGE_RESPONSE:
                        case MESSAGE_ERROR:
                            if (data.containsKey(KEY_RESPONSE)) {
                                String json = data.getString(KEY_RESPONSE);
                                P response = BaseMessage.fromJson(json, responseType);
                                response.setSender(sender);
                                callbackEmitter.onNext(response);
                            }
                            break;
                        case MESSAGE_END_STREAM:
                            callbackEmitter.onComplete();
                            break;
                    }
                }
            }
        }
    }

    public RxMessengerClient(Context context, Intent serviceIntent, Class<P> responseType) {
        this.context = context;
        this.serviceIntent = serviceIntent;
        this.responseType = responseType;
    }

    public Observable<P> sendMessage(final Q message) {
        return Observable.create(new ObservableOnSubscribe<P>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<P> emitter) throws Exception {
                bindToService(serviceIntent).subscribe(new Consumer<MessengerConnection<Q, P>>() {
                    @Override
                    public void accept(MessengerConnection<Q, P> messengerConnection) throws Exception {
                        if (messengerConnection.isBound()) {
                            messengerConnection.sendMessage(message, emitter);
                            messengerConnection.shutDown();
                        } else {
                            emitter.onError(new RuntimeException("Unable to bind to rxMessengerService"));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Log.e(TAG, "Failed to bind to service", throwable);
                        emitter.onError(new RuntimeException("Failed to find service for binding"));
                    }
                });
            }
        });
    }

    private Observable<MessengerConnection<Q, P>> bindToService(Intent serviceIntent) {
        MessengerConnection<Q, P> messengerConnection = new MessengerConnection<>(this, responseType);
        context.bindService(serviceIntent, messengerConnection, Context.BIND_AUTO_CREATE);
        return messengerConnection.getConnectedObservable();
    }
}
