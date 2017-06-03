# RxMessenger

This project provides a library implementation that can be used to send messages to an Android
Service and receive responses from it as an Observable stream.

## Getting started

// TODO - once library is deployed to artifactory add info here

## Setup the service

Your service should extend the `AbstractRxMessengerService` class and provide an implementation of
the `handleRequest` method. The implementation of the service must also specify the message request
 and repsonse classes. These classes should be simple POJO classes that extend the `BaseMessage`
 class. The message classes will be serialised to and from JSON during sending to the service,
 therefore you should make sure these classes are capable of being converted to JSON. The library
 makes use of GSON to perform the serialisation.

```java
public class DemoService extends AbstractRxMessengerService<RequestMessage, ResponseMessage> {

    private static final String TAG = DemoService.class.getSimpleName();

    public DemoService() {
        super(RequestMessage.class);
    }

    @Override
    protected void handleRequest(RequestMessage requestMessage) {

        // Setup response message here and send reply to client
        ResponseMessage responseMessage = new ResponseMessage();

        // send message to client
        sendMessageToClient(requestMessage.getMessageId(), responseMessage);

        // once we have finished sending all messages we can let
        // the client know and end the stream here
        sendEndStreamMessageToClient(requestMessage.getMessageId());
    }
}
```

Once you have created your service class it must be registered in the AndroidManifest.xml of your
application.

```xml
    <service android:name=".DemoService">
        <intent-filter>
            <action android:name="com.annalytics.rxmsg.action.DEMO_SERVER"/>
        </intent-filter>
    </service>
```

If required the service can enforce the use of permissions or any other standard Android service
settings.

## Setting up the client

The client application should use the `RxMessengerClient` class to communicate with the service. This
can be done anywhere within your application.

```java

    private static final String SERVER_ACTION = "com.annalytics.rxmsg.action.DEMO_SERVER";
    private static final String SERVER_PACKAGE = "com.annalytics.rxmsg.demoserver";
    private static final String SERVER_CLASS = "com.annalytics.rxmsg.demoserver.DemoService";

    private RxMessengerClient<RequestMessage, ResponseMessage> rxMessengerClient;

    protected void setup() {
        rxMessengerClient = new RxMessengerClient<>(this, getServerIntent(), ResponseMessage.class);
    }

    private Intent getServerIntent() {
        Intent intent = new Intent(SERVER_ACTION);
        intent.setClassName(SERVER_PACKAGE, SERVER_CLASS);
        return intent;
    }

    public void sendMessage() {
        RequestMessage requestMessage = new RequestMessage();

        rxMessengerClient.sendMessage(requestMessage)
                .subscribe(new Consumer<ResponseMessage>() {
            @Override
            public void accept(@NonNull ResponseMessage responseMessage) throws Exception {
                Log.d(TAG, "Got message from server: " + responseMessage.getServerMessage());
                // handle response here
            }
        });
    }

```

As you can see from the example above the client simply calls `sendMessage` passing the message object
to be sent and subscribes to the Obversable to receive the response. If the response is going to
be used to update your UI then you can use RxAndroid schedulers to ensure the message is received
on the UI thread. e.g.


```java

    rxMessengerClient.sendMessage(requestMessage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<ResponseMessage>() {
                // ...
            });

```
