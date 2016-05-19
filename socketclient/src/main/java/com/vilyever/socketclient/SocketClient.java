package com.vilyever.socketclient;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;

import com.vilyever.socketclient.helper.HeartBeatHelper;
import com.vilyever.socketclient.helper.PollingHelper;
import com.vilyever.socketclient.helper.SocketInputReader;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetNames;
import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.StringValidation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SocketClient
 * AndroidSocketClient <com.vilyever.socketclient>
 * Created by vilyever on 2016/3/18.
 * Feature:
 */
public class SocketClient {
    final SocketClient self = this;

    public static final int DefaultConnectionTimeout = 1000 * 15;

    /* Constructors */
    public SocketClient(@NonNull String remoteIP, int remotePort) {
        this(remoteIP, remotePort, DefaultConnectionTimeout);
    }

    public SocketClient(@NonNull String remoteIP, int remotePort, int connectionTimeout) {
        setRemoteIP(remoteIP);
        setRemotePort(remotePort);
        setConnectionTimeout(connectionTimeout);
    }

    /* Public Methods */
    public void connect() {
        if (!isDisconnected()) {
            return;
        }

        setState(State.Connecting);
        getConnectionThread().start();
    }

    public void disconnect() {
        if (isDisconnected() || isDisconnecting()) {
            return;
        }

        setDisconnecting(true);
        Log.d("logger", "disconnect " + getClass().getSimpleName());

        if (!getRunningSocket().isClosed()
                || isConnecting()) {
            try {
                getRunningSocket().getOutputStream().close();
                getRunningSocket().getInputStream().close();
            }
            catch (IOException e) {
//                e.printStackTrace();
            }
            finally {
                try {
                    getRunningSocket().close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                this.runningSocket = null;
            }
        }

        if (this.connectionThread != null) {
            this.connectionThread.interrupt();
            this.connectionThread = null;
        }
        if (this.sendThread != null) {
            this.sendThread.interrupt();
            this.sendThread = null;
        }
        if (this.receiveThread != null) {
            this.receiveThread.interrupt();
            this.receiveThread = null;
        }

        getUiHandler().sendEmptyMessage(UIHandler.MessageType.Disconnected.what());
    }

    /**
     * 与{@link #sendString(String)} 相同，增加一个别名
     * @return
     */
    public SocketPacket send(String message) {
        return sendString(message);
    }

    public SocketPacket sendString(String message) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(message);
        getSendThread().enqueueSocketPacket(socketPacket);
        return socketPacket;
    }

    /**
     * @param data
     * @return
     */
    public SocketPacket send(byte[] data) {
        return sendData(data);
    }

    /**
     * 与{@link #send(byte[])} 相同，增加一个别名
     * @param data
     * @return
     */
    public SocketPacket sendBytes(byte[] data) {
        return sendData(data);
    }

    /**
     * @param data
     * @return
     */
    public SocketPacket sendData(byte[] data) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(data);
        getSendThread().enqueueSocketPacket(socketPacket);
        return socketPacket;
    }

    public void cancelSend(SocketPacket socketPacket) {
        cancelSend(socketPacket.getID());
    }

    public void cancelSend(int socketPacketID) {
        getSendThread().cancel(socketPacketID);
    }

    public boolean isConnected() {
        return getState() == State.Connected;
    }

    public boolean isDisconnected() {
        return getState() == State.Disconnected;
    }

    public boolean isConnecting() {
        return getState() == State.Connecting;
    }

    /**
     * 注册监听回调
     * @param delegate 回调接收者
     */
    public SocketClient registerSocketDelegate(SocketDelegate delegate) {
        if (!getSocketDelegates().contains(delegate)) {
            getSocketDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册监听回调
     * @param delegate 回调接收者
     */
    public SocketClient removeSocketDelegate(SocketDelegate delegate) {
        getSocketDelegates().remove(delegate);
        return this;
    }

    /**
     * 注册心跳包监听回调
     * @param heartBeatDelegate 回调接收者
     */
    public SocketClient registerSocketHeartBeatDelegate(SocketHeartBeatDelegate heartBeatDelegate) {
        if (!getSocketHeartBeatDelegates().contains(heartBeatDelegate)) {
            getSocketHeartBeatDelegates().add(heartBeatDelegate);
        }
        return this;
    }

    /**
     * 取消注册心跳包监听回调
     * @param heartBeatDelegate 回调接收者
     */
    public SocketClient removeSocketHeartBeatDelegate(SocketDelegate heartBeatDelegate) {
        getSocketHeartBeatDelegates().remove(heartBeatDelegate);
        return this;
    }

    /**
     * 注册自动应答监听回调
     * @param pollingDelegate 回调接收者
     */
    public SocketClient registerSocketPollingDelegate(SocketPollingDelegate pollingDelegate) {
        if (!getSocketPollingDelegate().contains(pollingDelegate)) {
            getSocketPollingDelegate().add(pollingDelegate);
        }
        return this;
    }

    /**
     * 取消注册自动应答监听回调
     * @param pollingDelegate 回调接收者
     */
    public SocketClient removeSocketPollingDelegate(SocketPollingDelegate pollingDelegate) {
        getSocketPollingDelegate().remove(pollingDelegate);
        return this;
    }

    /* Properties */
    private Socket runningSocket;
    public Socket getRunningSocket() {
        if (this.runningSocket == null) {
            this.runningSocket = new Socket();
        }
        return this.runningSocket;
    }
    protected SocketClient setRunningSocket(Socket socket) {
        this.runningSocket = socket;
        return this;
    }

    /**
     * 远程IP
     */
    private String remoteIP;
    public SocketClient setRemoteIP(String remoteIP) {
        if (!StringValidation.validateRegex(remoteIP, StringValidation.RegexIP)) {
            ExceptionThrower.throwIllegalStateException("we need a correct remote IP to connect");
        }
        this.remoteIP = remoteIP;
        return this;
    }
    public String getRemoteIP() {
        return this.remoteIP;
    }

    /**
     * 远程端口
     */
    private int remotePort;
    public SocketClient setRemotePort(int remotePort) {
        if (!StringValidation.validateRegex(String.format("%d", remotePort), StringValidation.RegexPort)) {
            ExceptionThrower.throwIllegalStateException("we need a correct remote port to connect");
        }
        this.remotePort = remotePort;
        return this;
    }
    public int getRemotePort() {
        return this.remotePort;
    }

    /**
     * 连接超时时间
     */
    private int connectionTimeout;
    public SocketClient setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout < 0) {
            throw new IllegalArgumentException("we need connectionTimeout > 0");
        }
        this.connectionTimeout = connectionTimeout;
        return this;
    }
    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    /**
     * 当前连接状态
     * 当设置状态为{@link State#Connected}, 收发线程等初始操作均未启动
     * 此状态仅为一个标识
     */
    private State state;
    protected SocketClient setState(State state) {
        this.state = state;
        return this;
    }
    public State getState() {
        if (this.state == null) {
            return State.Disconnected;
        }
        return this.state;
    }

    /**
     * 设置默认的编码格式
     */
    private String charsetName;
    public SocketClient setCharsetName(String charsetName) {
        if (charsetName == null) {
            charsetName = CharsetNames.UTF_8;
        }
        this.charsetName = charsetName;
        getSocketPacketHelper().setCharsetName(charsetName);
        getHeartBeatHelper().setCharsetName(charsetName);
        getPollingHelper().setCharsetName(charsetName);
        return this;
    }
    public String getCharsetName() {
        if (this.charsetName == null) {
            this.charsetName = CharsetNames.UTF_8;
        }
        return this.charsetName;
    }

    /**
     * 发送接收时对信息的处理
     * 发送添加尾部信息
     * 接收使用尾部信息截断消息
     */
    private SocketPacketHelper socketPacketHelper;
    public SocketClient setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
        this.socketPacketHelper = socketPacketHelper;
        return this;
    }
    public SocketPacketHelper getSocketPacketHelper() {
        if (this.socketPacketHelper == null) {
            this.socketPacketHelper = new SocketPacketHelper(getCharsetName());
        }
        return this.socketPacketHelper;
    }

    /**
     * 心跳包信息
     */
    private HeartBeatHelper heartBeatHelper;
    public SocketClient setHeartBeatHelper(HeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper;
        return this;
    }
    public HeartBeatHelper getHeartBeatHelper() {
        if (this.heartBeatHelper == null) {
            this.heartBeatHelper = new HeartBeatHelper(getCharsetName());
        }
        return this.heartBeatHelper;
    }

    /**
     * 自动应答
     */
    private PollingHelper pollingHelper;
    public SocketClient setPollingHelper(PollingHelper pollingHelper) {
        this.pollingHelper = pollingHelper;
        return this;
    }
    public PollingHelper getPollingHelper() {
        if (this.pollingHelper == null) {
            this.pollingHelper = new PollingHelper(getCharsetName());
        }
        return this.pollingHelper;
    }

    /**
     * 计时器
     */
    private CountDownTimer hearBeatCountDownTimer;
    protected CountDownTimer getHearBeatCountDownTimer() {
        if (this.hearBeatCountDownTimer == null) {
            this.hearBeatCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000l) {
                @Override
                public void onTick(long millisUntilFinished) {
                    self.internalOnTimeTick();
                }

                @Override
                public void onFinish() {
                    this.start();
                }
            };

        }
        return this.hearBeatCountDownTimer;
    }

    private ConnectionThread connectionThread;
    protected ConnectionThread getConnectionThread() {
        if (this.connectionThread == null) {
            this.connectionThread = new ConnectionThread();
        }
        return this.connectionThread;
    }

    private SendThread sendThread;
    protected SendThread getSendThread() {
        if (this.sendThread == null) {
            this.sendThread = new SendThread();
        }
        return this.sendThread;
    }

    private ReceiveThread receiveThread;
    protected ReceiveThread getReceiveThread() {
        if (this.receiveThread == null) {
            this.receiveThread = new ReceiveThread();
        }
        return this.receiveThread;
    }

    private ArrayList<SocketDelegate> socketDelegates;
    protected ArrayList<SocketDelegate> getSocketDelegates() {
        if (this.socketDelegates == null) {
            this.socketDelegates = new ArrayList<SocketDelegate>();
        }
        return this.socketDelegates;
    }

    public interface SocketDelegate {
        void onConnected(SocketClient client);
        void onDisconnected(SocketClient client);
        void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket);

        class SimpleSocketDelegate implements SocketDelegate {
            @Override
            public void onConnected(SocketClient client) {

            }

            @Override
            public void onDisconnected(SocketClient client) {

            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

            }
        }
    }

    private ArrayList<SocketHeartBeatDelegate> socketHeartBeatDelegates;
    protected ArrayList<SocketHeartBeatDelegate> getSocketHeartBeatDelegates() {
        if (this.socketHeartBeatDelegates == null) {
            this.socketHeartBeatDelegates = new ArrayList<SocketHeartBeatDelegate>();
        }
        return this.socketHeartBeatDelegates;
    }
    public interface SocketHeartBeatDelegate {
        void onHeartBeat(SocketClient socketClient);

        class SimpleSocketHeartBeatDelegate implements SocketHeartBeatDelegate {
            @Override
            public void onHeartBeat(SocketClient socketClient) {

            }
        }
    }

    private ArrayList<SocketPollingDelegate> socketPollingDelegate;
    protected ArrayList<SocketPollingDelegate> getSocketPollingDelegate() {
        if (this.socketPollingDelegate == null) {
            this.socketPollingDelegate = new ArrayList<SocketPollingDelegate>();
        }
        return this.socketPollingDelegate;
    }
    public interface SocketPollingDelegate {
        void onPollingQuery(SocketClient socketClient, SocketResponsePacket pollingQueryPacket);
        void onPollingResponse(SocketClient socketClient, SocketResponsePacket pollingResponsePacket);

        class SimpleSocketPollingDelegate implements SocketPollingDelegate {
            @Override
            public void onPollingQuery(SocketClient socketClient, SocketResponsePacket pollingQueryPacket) {

            }

            @Override
            public void onPollingResponse(SocketClient socketClient, SocketResponsePacket pollingResponsePacket) {

            }
        }
    }

    private UIHandler uiHandler;
    protected UIHandler getUiHandler() {
        if (this.uiHandler == null) {
            this.uiHandler = new UIHandler(this);
        }
        return this.uiHandler;
    }
    protected static class UIHandler extends Handler {
        private WeakReference<SocketClient> referenceSocketClient;

        public UIHandler(@NonNull SocketClient referenceSocketClient) {
            super(Looper.getMainLooper());

            this.referenceSocketClient = new WeakReference<SocketClient>(referenceSocketClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (MessageType.typeFromWhat(msg.what)) {
                case Connected:
                    this.referenceSocketClient.get().internalOnConnected();
                    break;
                case Disconnected:
                    this.referenceSocketClient.get().internalOnDisconnected();
                    break;
                case ReceiveResponse:
                    this.referenceSocketClient.get().internalOnReceiveResponse((SocketResponsePacket) msg.obj);
                    break;
            }
        }

        public enum MessageType {
            Connected, Disconnected, ReceiveResponse;

            public static MessageType typeFromWhat(int what) {
                return MessageType.values()[what];
            }

            public int what() {
                return this.ordinal();
            }
        }
    }

    private boolean disconnecting;
    protected SocketClient setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
        return this;
    }
    protected boolean isDisconnecting() {
        return this.disconnecting;
    }

    /* Overrides */
     
     
    /* Delegates */

    /* Protected Methods */
    @UiThread
    @CallSuper
    protected void internalOnConnected() {
        setState(State.Connected);

        getSendThread().start();
        getReceiveThread().start();

        getHeartBeatHelper().setLastSendHeartBeatMessageTime(System.currentTimeMillis());
        getHeartBeatHelper().setLastReceiveMessageTime(System.currentTimeMillis());

        getHearBeatCountDownTimer().start();


        ArrayList<SocketDelegate> delegatesCopy =
                (ArrayList<SocketDelegate>) getSocketDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onConnected(this);
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnDisconnected() {
        setDisconnecting(false);
        setState(State.Disconnected);

        getHearBeatCountDownTimer().cancel();

        ArrayList<SocketDelegate> delegatesCopy =
                (ArrayList<SocketDelegate>) getSocketDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onDisconnected(this);
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnReceiveResponse(@NonNull SocketResponsePacket responsePacket) {
        getHeartBeatHelper().setLastReceiveMessageTime(System.currentTimeMillis());

        if (responsePacket.isMatch(getHeartBeatHelper().getReceiveData())) {
            internalOnReceiveHeartBeat();
            return;
        }

        if (getPollingHelper().containsQuery(responsePacket.getData())) {
            internalOnReceivePollingQuery(responsePacket);
            return;
        }

        if (getPollingHelper().containsResponse(responsePacket.getData())) {
            internalOnReceivePollingResponse(responsePacket);
            return;
        }

        ArrayList<SocketDelegate> delegatesCopy =
                (ArrayList<SocketDelegate>) getSocketDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onResponse(this, responsePacket);
        }
    }

    protected void internalOnReceiveHeartBeat() {

        ArrayList<SocketHeartBeatDelegate> delegatesCopy =
                (ArrayList<SocketHeartBeatDelegate>) getSocketHeartBeatDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onHeartBeat(this);
        }
    }

    @CallSuper
    protected void internalOnReceivePollingQuery(SocketResponsePacket pollingQueryPacket) {
        send(getPollingHelper().getResponse(pollingQueryPacket.getData()));

        ArrayList<SocketPollingDelegate> delegatesCopy =
                (ArrayList<SocketPollingDelegate>) getSocketPollingDelegate().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onPollingQuery(this, pollingQueryPacket);
        }
    }

    protected void internalOnReceivePollingResponse(SocketResponsePacket pollingResponsePacket) {

        ArrayList<SocketPollingDelegate> delegatesCopy =
                (ArrayList<SocketPollingDelegate>) getSocketPollingDelegate().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onPollingResponse(this, pollingResponsePacket);
        }
    }

    @CallSuper
    protected void internalOnTimeTick() {
        long currentTime = System.currentTimeMillis();

        if (getHeartBeatHelper().shouldSendHeartBeat()) {
            if (currentTime - getHeartBeatHelper().getLastSendHeartBeatMessageTime() >= getHeartBeatHelper().getHeartBeatInterval()) {
                send(getHeartBeatHelper().getSendData());
                getHeartBeatHelper().setLastSendHeartBeatMessageTime(currentTime);
            }
        }

        if (getHeartBeatHelper().shouldAutoDisconnectWhenRemoteNoReplyAliveTimeout()) {
            if (currentTime - getHeartBeatHelper().getLastReceiveMessageTime() >= getHeartBeatHelper().getRemoteNoReplyAliveTimeout()) {
                disconnect();
            }
        }
    }
     
    /* Private Methods */
    

    /* Enums */
    public enum State {
        Disconnected, Connecting, Connected
    }

    /* Inner Classes */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                self.getRunningSocket().connect(new InetSocketAddress(self.getRemoteIP(), self.getRemotePort()), self.getConnectionTimeout());
                self.getUiHandler().sendEmptyMessage(UIHandler.MessageType.Connected.what());
            }
            catch (IOException e) {
                e.printStackTrace();

                self.disconnect();
            }
        }
    }

    private class SendThread extends Thread {
        private final Object sendLock = new Object();

        public SendThread() {
        }

        private LinkedBlockingQueue<SocketPacket> sendingQueue;
        protected LinkedBlockingQueue<SocketPacket> getSendingQueue() {
            if (sendingQueue == null) {
                sendingQueue = new LinkedBlockingQueue<SocketPacket>();
            }
            return sendingQueue;
        }

        public void enqueueSocketPacket(final SocketPacket socketPacket) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        getSendingQueue().put(socketPacket);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);

                }
            }.execute();
        }

        public void cancel(int socketPacketID) {
            Iterator<SocketPacket> iterator = getSendingQueue().iterator();
            while (iterator.hasNext()) {
                SocketPacket packet = iterator.next();
                if (packet.getID() == socketPacketID) {
                    iterator.remove();
                    break;
                }
            }
        }

        @Override
        public void run() {
            super.run();
            while (self.isConnected() && !Thread.interrupted()) {
                SocketPacket packet;
                try {
                    while ((packet = getSendingQueue().take()) != null) {
                        byte[] data = packet.getData();
                        if (data == null && packet.getMessage() != null) {
                            try {
                                String message = packet.getMessage();
                                data = message.getBytes(self.getCharsetName());
                            }
                            catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                        if (data != null) {
                            try {
                                byte[] tailData = self.getSocketPacketHelper().getSendTailData();
                                if (tailData != null) {
                                    data = Arrays.copyOf(data, data.length + tailData.length);
                                    for (int i = 0; i < tailData.length; i++) {
                                        data[data.length - tailData.length + i] = tailData[i];
                                    }
                                }
                                self.getRunningSocket().getOutputStream().write(data);
                                self.getRunningSocket().getOutputStream().flush();
                            }
                            catch (IOException e) {
//                                e.printStackTrace();
                            }
                        }
                    }
                }
                catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();

            SocketInputReader inputReader = null;
            try {
                inputReader = new SocketInputReader(self.getRunningSocket().getInputStream());

                while (self.isConnected() && !Thread.interrupted()) {
                    byte[] result = inputReader.readBytes(self.getSocketPacketHelper().getReceiveTailData());
                    if (result == null) {
                        self.disconnect();
                        break;
                    }

                    String resultMessage = null;
                    try {
                        resultMessage = new String(result, Charset.forName(self.getCharsetName()));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    SocketResponsePacket responsePacket = new SocketResponsePacket(result, resultMessage);

                    Message message = Message.obtain();
                    message.what = UIHandler.MessageType.ReceiveResponse.what();
                    message.obj = responsePacket;
                    self.getUiHandler().sendMessage(message);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                self.disconnect();
            }
        }
    }

}