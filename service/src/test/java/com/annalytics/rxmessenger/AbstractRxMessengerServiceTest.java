package com.annalytics.rxmessenger;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import io.reactivex.annotations.NonNull;

import static com.annalytics.rxmessenger.AbstractRxMessengerService.KEY_DATA_REQUEST;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.KEY_DATA_RESPONSE;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_REQUEST;
import static com.annalytics.rxmessenger.AbstractRxMessengerService.MESSAGE_RESPONSE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@Config(sdk = Build.VERSION_CODES.LOLLIPOP, manifest = Config.NONE, shadows = {MockShadowMessenger.class})
@RunWith(RobolectricTestRunner.class)
public class AbstractRxMessengerServiceTest {

    TestAbstractRxMessengerService testAbstractMessengerService;

    @Mock
    Messenger clientMessenger;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        initMocks(this);
        MockShadowMessenger.clearMessages();
        testAbstractMessengerService = new TestAbstractRxMessengerService(DataObject.class);
    }

    @Test
    public void checkWillIgnoreEmptyMessage() throws RemoteException {
        Message m = setupEmptyMessage();

        testAbstractMessengerService.incomingMessenger.send(m);

        assertThat(testAbstractMessengerService.lastAction).isNull();
    }

    @Test
    public void checkWillIgnoreInvalidJsonMessage() throws RemoteException {
        Message m = setupJsonMessage("{ status: INIT; }");

        testAbstractMessengerService.incomingMessenger.send(m);

        assertThat(testAbstractMessengerService.lastAction).isNull();
    }

    @Test
    public void checkWillIgnoreActionNullMessage() throws RemoteException {
        Message m = setupJsonMessage(null);

        testAbstractMessengerService.incomingMessenger.send(m);

        assertThat(testAbstractMessengerService.lastAction).isNull();
    }

    @Test
    public void checkWillHandleValidMessage() throws RemoteException {
        Message m = setupJsonMessage("{ messageId: 567 }");

        testAbstractMessengerService.incomingMessenger.send(m);

        assertThat(testAbstractMessengerService.lastAction.getMessageId()).isEqualTo("567");
    }

    @Test
    public void willHandleValidMessage() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        assertThat(testAbstractMessengerService.lastAction).isEqualTo(dataObject);
    }

    @Test
    public void canSendMessageToClient() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        DataObject response = new DataObject();
        testAbstractMessengerService.sendMessageToClient(dataObject.getMessageId(), response);

        verifyDataSentToClient(response);
    }

    @Test
    public void wontSendNullMessageToClient() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        testAbstractMessengerService.sendMessageToClient(dataObject.getMessageId(), null);

        verify(clientMessenger, times(0)).send(any(Message.class));
    }

    @Test
    public void wontSendNullIdMessageToClient() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        testAbstractMessengerService.sendMessageToClient(null, new DataObject());

        verify(clientMessenger, times(0)).send(any(Message.class));
    }

    @Test
    public void wontSendUnknownIdMessageToClient() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        testAbstractMessengerService.sendMessageToClient("6767", new DataObject());

        verify(clientMessenger, times(0)).send(any(Message.class));
    }

    @Test
    public void checkClientMessengerThrowIsHandledSafely() throws RemoteException {
        DataObject dataObject = new DataObject();
        receiveServiceMessage(dataObject);

        doThrow(new RemoteException("Argh aliens!!")).when(clientMessenger).send(any(Message.class));

        testAbstractMessengerService.sendMessageToClient(dataObject.getMessageId(), new DataObject());
    }

    private void verifyDataSentToClient(DataObject msg) throws RemoteException {
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(clientMessenger).send(messageCaptor.capture());

        Message m = messageCaptor.getValue();
        Bundle b = m.getData();
        Assertions.assertThat(b).isNotNull();
        Assertions.assertThat(m.what).isEqualTo(MESSAGE_RESPONSE);
        Assertions.assertThat(b.getString(KEY_DATA_RESPONSE)).isNotNull();
        Assertions.assertThat(b.getString(KEY_DATA_RESPONSE)).isEqualTo(msg.toJson());
    }

    @NonNull
    private Message setupEmptyMessage() {
        return Message.obtain();
    }

    @NonNull
    private Message setupJsonMessage(String json) {
        Message m = Message.obtain();
        m.what = MESSAGE_REQUEST;
        m.replyTo = clientMessenger;
        Bundle b = new Bundle();
        b.putString(KEY_DATA_REQUEST, json);
        m.setData(b);
        return m;
    }

    @NonNull
    private void receiveServiceMessage(DataObject data) throws RemoteException {
        String json = data.toJson();
        Message m = setupJsonMessage(json);
        testAbstractMessengerService.incomingMessenger.send(m);
    }

    class DataObject extends BaseMessage {

    }

    class TestAbstractRxMessengerService extends AbstractRxMessengerService<DataObject, DataObject> {

        DataObject lastAction;

        protected TestAbstractRxMessengerService(Class<DataObject> requestType) {
            super(requestType);
            attachBaseContext(RuntimeEnvironment.application);
        }

        @Override
        protected void handleRequest(DataObject action) {
            lastAction = action;
        }
    }
}
