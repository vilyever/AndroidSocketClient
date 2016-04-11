package com.vilyever.socketclient;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.vilyever.socketclient.util.CharsetNames;
import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.SocketInputReader;
import com.vilyever.socketclient.util.StringValidation;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    public static final long DefaultHeartBeatInterval = 1000 * 30;
    public static final long DefaultRemoteNoReplyAliveTimeout = DefaultHeartBeatInterval * 2;

    /* Constructors */
    public SocketClient(@NonNull String remoteIP, int remotePort) {
        this(remoteIP, remotePort, DefaultConnectionTimeout);
    }

    public SocketClient(@NonNull String remoteIP, int remotePort, int connectionTimeout) {
        setRemoteIP(remoteIP);
        setRemotePort(remotePort);
        setConnectionTimeout(connectionTimeout);

        registerQueryResponse(SocketPacket.DefaultPollingQueryMessage, SocketPacket.DefaultPollingResponseMessage);
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
        if (isDisconnected()) {
            return;
        }

        if (!getRunningSocket().isClosed()) {
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
//                    e.printStackTrace();
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
     * @param data
     * @return
     */
    public SocketPacket send(byte[] data) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(data);
        getSendThread().enqueueSocketPacket(socketPacket);
        return socketPacket;
    }

    public SocketPacket send(String message) {
        return send(message, getCharsetName());
    }

    public SocketPacket send(String message, String charsetName) {
        return send(message, Charset.forName(charsetName));
    }

    public SocketPacket send(String message, Charset charset) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(message, charset, isSupportReadLine());
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

    public SocketClient registerQueryResponse(String query, String response) {
        getQueryResponseMap().put(query, response);
        return this;
    }

    public SocketClient registerQueryResponse(HashMap<String, String> queryResponseMap) {
        getQueryResponseMap().putAll(queryResponseMap);
        return this;
    }

    public SocketClient removeQueryResponse(String query) {
        getQueryResponseMap().remove(query);
        return this;
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
    protected Socket getRunningSocket() {
        if (this.runningSocket == null) {
            this.runningSocket = new Socket();
        }
        return this.runningSocket;
    }
    protected SocketClient setRunningSocket(Socket socket) {
        this.runningSocket = socket;
        return this;
    }

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
     * 设置是否支持按行读取消息
     * 若否则读取每一次缓冲返回一次消息
     * 即受到的消息末尾是 '\r\n' 符号
     * 此操作可以解决发送方发送过快时缓冲池内存有多条信息
     */
    private boolean supportReadLine = true;
    public SocketClient setSupportReadLine(boolean supportReadLine) {
        this.supportReadLine = supportReadLine;
        return this;
    }
    public boolean isSupportReadLine() {
        return this.supportReadLine;
    }

    /**
     * 设置默认的编码格式
     */
    private String charsetName;
    public SocketClient setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        return this;
    }
    public String getCharsetName() {
        if (this.charsetName == null) {
            this.charsetName = CharsetNames.UTF_8;
        }
        return this.charsetName;
    }

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
     * 远程端在一定时间间隔没有消息后自动断开
     */
    private long remoteNoReplyAliveTimeout = DefaultRemoteNoReplyAliveTimeout;
    public SocketClient setRemoteNoReplyAliveTimeout(long remoteNoReplyAliveTimeout) {
        if (remoteNoReplyAliveTimeout < 0) {
            throw new IllegalArgumentException("we need remoteNoReplyAliveTimeout > 0");
        }
        this.remoteNoReplyAliveTimeout = remoteNoReplyAliveTimeout;
        return this;
    }
    public long getRemoteNoReplyAliveTimeout() {
        return this.remoteNoReplyAliveTimeout;
    }

    /**
     * 心跳包信息
     */
    private String heartBeatMessage;
    public SocketClient setHeartBeatMessage(String heartBeatMessage) {
        this.heartBeatMessage = heartBeatMessage;
        return this;
    }
    public String getHeartBeatMessage() {
        if (this.heartBeatMessage == null) {
            this.heartBeatMessage = SocketPacket.DefaultHeartBeatMessage;
        }
        return this.heartBeatMessage;
    }

    /**
     * 心跳包发送间隔
     */
    private long heartBeatInterval = DefaultHeartBeatInterval;
    public SocketClient setHeartBeatInterval(long heartBeatInterval) {
        if (heartBeatInterval < 0) {
            throw new IllegalArgumentException("we need heartBeatInterval > 0");
        }
        this.heartBeatInterval = heartBeatInterval;
        return this;
    }
    public long getHeartBeatInterval() {
        return this.heartBeatInterval;
    }

    private long lastSendHeartBeatMessageTime;
    protected SocketClient setLastSendHeartBeatMessageTime(long lastSendHeartBeatMessageTime) {
        this.lastSendHeartBeatMessageTime = lastSendHeartBeatMessageTime;
        return this;
    }
    protected long getLastSendHeartBeatMessageTime() {
        return this.lastSendHeartBeatMessageTime;
    }

    private long lastReceiveMessageTime;
    protected SocketClient setLastReceiveMessageTime(long lastReceiveMessageTime) {
        this.lastReceiveMessageTime = lastReceiveMessageTime;
        return this;
    }
    protected long getLastReceiveMessageTime() {
        return this.lastReceiveMessageTime;
    }

    /**
     * 心跳包发送计时器
     */
    private CountDownTimer hearBeatCountDownTimer;
    protected CountDownTimer getHearBeatCountDownTimer() {
        if (this.hearBeatCountDownTimer == null) {
            this.hearBeatCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000l) {
                @Override
                public void onTick(long millisUntilFinished) {
                    self.onTimeTick();
                }

                @Override
                public void onFinish() {
                    self.getHearBeatCountDownTimer().start();
                }
            };

        }
        return this.hearBeatCountDownTimer;
    }

    /**
     * 当前连接状态
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
     * 自动应答键值对
     */
    private HashMap<String, String> queryResponseMap;
    protected HashMap<String, String> getQueryResponseMap() {
        if (this.queryResponseMap == null) {
            this.queryResponseMap = new HashMap<String, String>();
        }
        return this.queryResponseMap;
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
        void onPollingQuery(SocketClient socketClient, String pollingQuery);
        void onPollingResponse(SocketClient socketClient, String pollingResponse);

        class SimpleSocketPollingDelegate implements SocketPollingDelegate {
            @Override
            public void onPollingQuery(SocketClient socketClient, String pollingQuery) {

            }

            @Override
            public void onPollingResponse(SocketClient socketClient, String pollingResponse) {

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
                    this.referenceSocketClient.get().onConnected();
                    break;
                case Disconnected:
                    this.referenceSocketClient.get().onDisconnected();
                    break;
                case ReceiveResponse:
                    this.referenceSocketClient.get().onReceiveResponse((SocketResponsePacket) msg.obj);
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

    /* Overrides */
     
     
    /* Delegates */

    /* Protected Methods */
    @UiThread
    @CallSuper
    protected void onConnected() {
        setState(State.Connected);

        getSendThread().start();
        getReceiveThread().start();

        send(getHeartBeatMessage());
        setLastSendHeartBeatMessageTime(System.currentTimeMillis());
        setLastReceiveMessageTime(System.currentTimeMillis());

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
    protected void onDisconnected() {
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
    protected void onReceiveResponse(@NonNull SocketResponsePacket responsePacket) {
        setLastReceiveMessageTime(System.currentTimeMillis());

        if (responsePacket.isMatch(getHeartBeatMessage())) {
            onReceiveHeartBeat();
            return;
        }

        for (Map.Entry<String, String> entry : getQueryResponseMap().entrySet()) {
            if (responsePacket.isMatch(entry.getKey())) {
                onReceivePollingQuery(entry.getKey());
                return;
            }
            if (responsePacket.isMatch(entry.getValue())) {
                onReceivePollingResponse(entry.getValue());
                return;
            }
        }

        ArrayList<SocketDelegate> delegatesCopy =
                (ArrayList<SocketDelegate>) getSocketDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onResponse(this, responsePacket);
        }
    }

    protected void onReceiveHeartBeat() {

        ArrayList<SocketHeartBeatDelegate> delegatesCopy =
                (ArrayList<SocketHeartBeatDelegate>) getSocketHeartBeatDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onHeartBeat(this);
        }
    }

    @CallSuper
    protected void onReceivePollingQuery(String pollingQuery) {
        send(getQueryResponseMap().get(pollingQuery));

        ArrayList<SocketPollingDelegate> delegatesCopy =
                (ArrayList<SocketPollingDelegate>) getSocketPollingDelegate().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onPollingQuery(this, pollingQuery);
        }
    }

    protected void onReceivePollingResponse(String pollingResponse) {

        ArrayList<SocketPollingDelegate> delegatesCopy =
                (ArrayList<SocketPollingDelegate>) getSocketPollingDelegate().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onPollingResponse(this, pollingResponse);
        }
    }

    @CallSuper
    protected void onTimeTick() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - getLastSendHeartBeatMessageTime() >= getHeartBeatInterval()) {
            send(getHeartBeatMessage());
            setLastSendHeartBeatMessageTime(currentTime);
        }

        if (currentTime - getLastReceiveMessageTime() >= getRemoteNoReplyAliveTimeout()) {
            disconnect();
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
                self.getRunningSocket().setTcpNoDelay(true);
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

        public void enqueueSocketPacket(SocketPacket socketPacket) {
            getSendingQueue().add(socketPacket);
            synchronized (this.sendLock) {
                this.sendLock.notifyAll();
            }
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
                while ((packet = getSendingQueue().poll()) != null) {
                    long lastSendTime = System.currentTimeMillis();
                    try {
                        self.getRunningSocket().getOutputStream().write(packet.getData());
                        self.getRunningSocket().getOutputStream().flush();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    // 若不支持换行符分割消息，增加每一条消息的发送间隔，此举在本地java端socket互连时可解决快速发送时多条消息混杂问题
                    if (!self.isSupportReadLine()) {
                        while (System.currentTimeMillis() - lastSendTime < 3) {
                            Thread.yield();
                        }
                    }
                }

                synchronized (this.sendLock) {
                    try {
                        this.sendLock.wait();
                    }
                    catch (InterruptedException e) {
//                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                SocketInputReader inputReader = new SocketInputReader(self.getRunningSocket().getInputStream());

                while (self.isConnected() && !Thread.interrupted()) {
                    byte[] result = inputReader.readBytes();
                    if (result == null) {
                        self.disconnect();
                        break;
                    }

                    SocketResponsePacket responsePacket = new SocketResponsePacket(result, self.getCharsetName());

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