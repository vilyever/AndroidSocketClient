package com.vilyever.socketclient;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;

import com.vilyever.socketclient.helper.HeartBeatHelper;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceiveDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketConfigure;
import com.vilyever.socketclient.helper.SocketInputReader;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;
import com.vilyever.socketclient.util.ExceptionThrower;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

    public static final String TAG = SocketClient.class.getSimpleName();

    /* Constructors */
    public SocketClient() {
        this(null);
    }

    public SocketClient(SocketClientAddress address) {
        this.address = address;
    }

    /* Public Methods */
    public void connect() {
        if (!isDisconnected()) {
            return;
        }

        if (getAddress() == null) {
            ExceptionThrower.throwIllegalStateException("we need a SocketClientAddress to connect");
        }

        getAddress().checkValidation();

        getSocketConfigure().setCharsetName(getCharsetName()).setHeartBeatHelper(getHeartBeatHelper()).setSocketPacketHelper(getSocketPacketHelper());
        setState(State.Connecting);
        getConnectionThread().start();
    }

    public void disconnect() {
        if (isDisconnected() || isDisconnecting()) {
            return;
        }

        setDisconnecting(true);

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
     * 发送字符串，将以{@link #charsetName}编码为byte数组
     * @param message
     * @return 打包后的数据包
     */
    public SocketPacket sendString(String message) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(message);
        getSendThread().enqueueSocketPacket(socketPacket);
        return socketPacket;
    }

    /**
     * 发送byte数组
     * @param data
     * @return 打包后的数据包
     */
    public SocketPacket sendData(byte[] data) {
        if (!isConnected()) {
            return null;
        }
        SocketPacket socketPacket = new SocketPacket(data);
        getSendThread().enqueueSocketPacket(socketPacket);
        return socketPacket;
    }

    public SocketPacket sendPacket(SocketPacket packet) {
        if (!isConnected()) {
            return null;
        }
        getSendThread().enqueueSocketPacket(packet);
        return packet;
    }

    public void cancelSend(SocketPacket socketPacket) {
        getSendThread().cancel(socketPacket);
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
    public SocketClient registerSocketClientDelegate(SocketClientDelegate delegate) {
        if (!getSocketClientDelegates().contains(delegate)) {
            getSocketClientDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册监听回调
     * @param delegate 回调接收者
     */
    public SocketClient removeSocketClientDelegate(SocketClientDelegate delegate) {
        getSocketClientDelegates().remove(delegate);
        return this;
    }

    /**
     * 注册信息发送回调
     * @param delegate 回调接收者
     */
    public SocketClient registerSocketClientSendingDelegate(SocketClientSendingDelegate delegate) {
        if (!getSocketClientSendingDelegates().contains(delegate)) {
            getSocketClientSendingDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册信息发送回调
     * @param delegate 回调接收者
     */
    public SocketClient removeSocketClientSendingDelegate(SocketClientSendingDelegate delegate) {
        getSocketClientSendingDelegates().remove(delegate);
        return this;
    }

    /**
     * 注册信息接收回调
     * @param delegate 回调接收者
     */
    public SocketClient registerSocketClientReceiveDelegate(SocketClientReceiveDelegate delegate) {
        if (!getSocketClientReceiveDelegates().contains(delegate)) {
            getSocketClientReceiveDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册信息接收回调
     * @param delegate 回调接收者
     */
    public SocketClient removeSocketClientReceiveDelegate(SocketClientReceiveDelegate delegate) {
        getSocketClientReceiveDelegates().remove(delegate);
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

    private SocketClientAddress address;
    public SocketClient setAddress(SocketClientAddress address) {
        this.address = address;
        return this;
    }
    public SocketClientAddress getAddress() {
        if (this.address == null) {
            this.address = new SocketClientAddress();
        }
        return this.address;
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
            charsetName = CharsetUtil.UTF_8;
        }
        this.charsetName = charsetName;
        getSocketPacketHelper().setCharsetName(charsetName);
        getHeartBeatHelper().setCharsetName(charsetName);
        return this;
    }
    public String getCharsetName() {
        if (this.charsetName == null) {
            this.charsetName = CharsetUtil.UTF_8;
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

    private SocketConfigure socketConfigure;
    protected SocketConfigure getSocketConfigure() {
        if (this.socketConfigure == null) {
            this.socketConfigure = new SocketConfigure();
        }
        return this.socketConfigure;
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
                    if (self.isConnected()) {
                        this.start();
                    }
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

    private ArrayList<SocketClientDelegate> socketClientDelegates;
    protected ArrayList<SocketClientDelegate> getSocketClientDelegates() {
        if (this.socketClientDelegates == null) {
            this.socketClientDelegates = new ArrayList<SocketClientDelegate>();
        }
        return this.socketClientDelegates;
    }

    private ArrayList<SocketClientSendingDelegate> socketClientSendingDelegates;
    protected ArrayList<SocketClientSendingDelegate> getSocketClientSendingDelegates() {
        if (this.socketClientSendingDelegates == null) {
            this.socketClientSendingDelegates = new ArrayList<SocketClientSendingDelegate>();
        }
        return this.socketClientSendingDelegates;
    }

    private ArrayList<SocketClientReceiveDelegate> socketClientReceiveDelegates;
    protected ArrayList<SocketClientReceiveDelegate> getSocketClientReceiveDelegates() {
        if (this.socketClientReceiveDelegates == null) {
            this.socketClientReceiveDelegates = new ArrayList<SocketClientReceiveDelegate>();
        }
        return this.socketClientReceiveDelegates;
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

            if (this.referenceSocketClient.get() == null) {
                return;
            }

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
                case SendingBegin:
                    this.referenceSocketClient.get().internalOnSendPacketBegin((SocketPacket) msg.obj);
                    break;
                case SendingCancel:
                    this.referenceSocketClient.get().internalOnSendPacketCancel((SocketPacket) msg.obj);
                    break;
                case SendingEnd:
                    this.referenceSocketClient.get().internalOnSendPacketEnd((SocketPacket) msg.obj);
                    break;
                case SendingProgress:
                    this.referenceSocketClient.get().internalOnSendPacketProgress((SocketPacket) msg.obj, msg.arg1 / 100.0f);
                    break;
            }
        }

        public enum MessageType {
            Connected, Disconnected, ReceiveResponse, SendingBegin, SendingCancel, SendingEnd, SendingProgress;

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

    /**
     * 记录上次发送心跳包的时间
     */
    private long lastSendHeartBeatMessageTime;
    protected SocketClient setLastSendHeartBeatMessageTime(long lastSendHeartBeatMessageTime) {
        this.lastSendHeartBeatMessageTime = lastSendHeartBeatMessageTime;
        return this;
    }
    protected long getLastSendHeartBeatMessageTime() {
        return this.lastSendHeartBeatMessageTime;
    }

    /**
     * 记录上次接收到消息的时间
     */
    private long lastReceiveMessageTime;
    protected SocketClient setLastReceiveMessageTime(long lastReceiveMessageTime) {
        this.lastReceiveMessageTime = lastReceiveMessageTime;
        return this;
    }
    protected long getLastReceiveMessageTime() {
        return this.lastReceiveMessageTime;
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

        setLastSendHeartBeatMessageTime(System.currentTimeMillis());
        setLastReceiveMessageTime(System.currentTimeMillis());

        getHearBeatCountDownTimer().start();


        ArrayList<SocketClientDelegate> delegatesCopy =
                (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
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

        ArrayList<SocketClientDelegate> delegatesCopy =
                (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onDisconnected(this);
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnReceiveResponse(@NonNull SocketResponsePacket responsePacket) {
        setLastReceiveMessageTime(System.currentTimeMillis());

        if (responsePacket.isMatch(getSocketConfigure().getHeartBeatHelper().getReceiveData())) {
            internalOnReceiveHeartBeat();
            return;
        }

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientDelegate> delegatesCopy =
                    (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onResponse(this, responsePacket);
            }
        }

        if (getSocketClientReceiveDelegates().size() > 0) {
            ArrayList<SocketClientReceiveDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceiveDelegate>) getSocketClientReceiveDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onResponse(this, responsePacket);
            }
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnReceiveHeartBeat() {

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientReceiveDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceiveDelegate>) getSocketClientReceiveDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onHeartBeat(this);
            }
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnSendPacketBegin(SocketPacket packet) {

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketBegin(this, packet);
            }
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnSendPacketCancel(SocketPacket packet) {

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketCancel(this, packet);
            }
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnSendPacketEnd(SocketPacket packet) {

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketEnd(this, packet);
            }
        }
    }

    @UiThread
    @CallSuper
    protected void internalOnSendPacketProgress(SocketPacket packet, float progress) {

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketProgress(this, packet, progress);
            }
        }
    }

    @CallSuper
    protected void internalOnTimeTick() {
        long currentTime = System.currentTimeMillis();

        if (getSocketConfigure().getHeartBeatHelper().shouldSendHeartBeat()) {
            if (currentTime - getLastSendHeartBeatMessageTime() >= getSocketConfigure().getHeartBeatHelper().getHeartBeatInterval()) {
                sendData(getSocketConfigure().getHeartBeatHelper().getSendData());
                setLastSendHeartBeatMessageTime(currentTime);
            }
        }

        if (getSocketConfigure().getHeartBeatHelper().shouldAutoDisconnectWhenRemoteNoReplyAliveTimeout()) {
            if (currentTime - getLastReceiveMessageTime() >= getSocketConfigure().getHeartBeatHelper().getRemoteNoReplyAliveTimeout()) {
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
                self.getRunningSocket().connect(new InetSocketAddress(self.getAddress().getRemoteIP(), self.getAddress().getRemotePort()), self.getAddress().getConnectionTimeout());
                self.getUiHandler().sendEmptyMessage(UIHandler.MessageType.Connected.what());
            }
            catch (IOException e) {
                e.printStackTrace();

                self.disconnect();
            }
        }
    }

    private class SendThread extends Thread {
        public SendThread() {
        }

        private LinkedBlockingQueue<SocketPacket> sendingPacketQueue;
        protected LinkedBlockingQueue<SocketPacket> getSendingPacketQueue() {
            if (sendingPacketQueue == null) {
                sendingPacketQueue = new LinkedBlockingQueue<SocketPacket>();
            }
            return sendingPacketQueue;
        }

        private SocketPacket sendingSocketPacket;
        protected SendThread setSendingSocketPacket(SocketPacket sendingSocketPacket) {
            this.sendingSocketPacket = sendingSocketPacket;
            return this;
        }
        public SocketPacket getSendingSocketPacket() {
            return this.sendingSocketPacket;
        }

        public void enqueueSocketPacket(final SocketPacket socketPacket) {
            if (getSendingSocketPacket() == socketPacket
                    || getSendingPacketQueue().contains(socketPacket)) {
                Log.w(TAG, "the socketPacket with ID " + socketPacket.getID() + " is already in sending queue.");
                return;
            }

            try {
                getSendingPacketQueue().put(socketPacket);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void cancel(SocketPacket packet) {
            if (getSendingSocketPacket() != null
                    && getSendingSocketPacket() == packet) {
                getSendingSocketPacket().setCanceled(true);
            }
            else {
                getSendingPacketQueue().remove(packet);
            }
        }

        public void cancel(int socketPacketID) {
            if (getSendingSocketPacket() != null
                    && getSendingSocketPacket().getID() == socketPacketID) {
                getSendingSocketPacket().setCanceled(true);
            }
            else {
                Iterator<SocketPacket> iterator = getSendingPacketQueue().iterator();
                while (iterator.hasNext()) {
                    SocketPacket packet = iterator.next();
                    if (packet.getID() == socketPacketID) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public void run() {
            super.run();
            SocketPacket packet;
            try {
                while (self.isConnected()
                       && !Thread.interrupted()
                        && (packet = getSendingPacketQueue().take()) != null) {
                    setSendingSocketPacket(packet);
                    packet.setCanceled(false);
                    packet.setSendingProgress(0.0f);

                    internalBeginSendPacket(packet);

                    internalUpdateSendProgress(packet);

                    if (!internalCheckShouldContinueSend(packet)) {
                        continue;
                    }

                    byte[] data = packet.getData();
                    if (data == null && packet.getMessage() != null) {
                        data = CharsetUtil.stringToData(packet.getMessage(), self.getSocketConfigure().getCharsetName());
                    }

                    if (data != null && data.length > 0) {
                        try {
                            // 发送包头
                            byte[] headerData = self.getSocketConfigure().getSocketPacketHelper().getSendHeaderData();
                            if (headerData != null) {
                                if (!internalCheckShouldContinueSend(packet)) {
                                    continue;
                                }
                                self.getRunningSocket().getOutputStream().write(headerData);
                                self.getRunningSocket().getOutputStream().flush();

                                packet.setSendingProgress(0.01f);
                                internalUpdateSendProgress(packet);
                            }

                            int segmentLength = self.getSocketConfigure().getSocketPacketHelper().getSegmentLength();
                            if (segmentLength == SocketPacketHelper.SegmentLengthMax) {
                                if (!internalCheckShouldContinueSend(packet)) {
                                    continue;
                                }
                                self.getRunningSocket().getOutputStream().write(data);
                                self.getRunningSocket().getOutputStream().flush();
                            }
                            else {
                                int offset = 0;

                                while (offset < data.length) {
                                    if (!internalCheckShouldContinueSend(packet)) {
                                        continue;
                                    }

                                    int end = offset + segmentLength;
                                    end = Math.min(data.length, end);
                                    self.getRunningSocket().getOutputStream().write(data, offset, end - offset);
                                    self.getRunningSocket().getOutputStream().flush();

                                    float progress = end / (float) data.length;
                                    progress = Math.max(0.01f, progress);
                                    progress = Math.min(0.99f, progress);

                                    packet.setSendingProgress(progress);
                                    internalUpdateSendProgress(packet);

                                    offset = end;
                                }
                            }

                            // 发送包尾
                            byte[] tailData = self.getSocketConfigure().getSocketPacketHelper().getSendTrailerData();
                            if (tailData != null) {
                                if (!internalCheckShouldContinueSend(packet)) {
                                    continue;
                                }
                                self.getRunningSocket().getOutputStream().write(tailData);
                                self.getRunningSocket().getOutputStream().flush();

                            }

                            packet.setSendingProgress(1.0f);
                            internalUpdateSendProgress(packet);

                            internalEndSendPacket(packet);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }

        private boolean internalCheckShouldContinueSend(SocketPacket packet) {
            if (packet.isCanceled() || Thread.interrupted()) {
                internalCancelSendPacket(packet);
                return false;
            }

            return true;
        }

        private void internalCancelSendPacket(SocketPacket packet) {
            Message message = Message.obtain();
            message.what = UIHandler.MessageType.SendingCancel.what();
            message.obj = packet;
            self.getUiHandler().sendMessage(message);
        }

        private void internalBeginSendPacket(SocketPacket packet) {
            Message message = Message.obtain();
            message.what = UIHandler.MessageType.SendingBegin.what();
            message.obj = packet;
            self.getUiHandler().sendMessage(message);
        }

        private void internalEndSendPacket(SocketPacket packet) {
            Message message = Message.obtain();
            message.what = UIHandler.MessageType.SendingEnd.what();
            message.obj = packet;
            self.getUiHandler().sendMessage(message);
        }

        private void internalUpdateSendProgress(SocketPacket packet) {
            Message message = Message.obtain();
            message.what = UIHandler.MessageType.SendingProgress.what();
            message.obj = packet;
            message.arg1 = (int) (packet.getSendingProgress() * 100);
            self.getUiHandler().sendMessage(message);
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
                    byte[] result = inputReader.readBytes(self.getSocketConfigure().getSocketPacketHelper().getReceiveHeaderData(), self.getSocketConfigure().getSocketPacketHelper().getReceiveTrailerData());
                    if (result == null) {
                        self.disconnect();
                        break;
                    }

                    String resultMessage = null;
                    try {
                        resultMessage = new String(result, Charset.forName(self.getSocketConfigure().getCharsetName()));
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