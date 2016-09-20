package com.vilyever.socketclient;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketConfigure;
import com.vilyever.socketclient.helper.SocketHeartBeatHelper;
import com.vilyever.socketclient.helper.SocketInputReader;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
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
        this(new SocketClientAddress());
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
            throw new IllegalArgumentException("we need a SocketClientAddress to connect");
        }

        getAddress().checkValidation();
        getSocketPacketHelper().checkValidation();

        getSocketConfigure().setCharsetName(getCharsetName()).setAddress(getAddress()).setHeartBeatHelper(getHeartBeatHelper()).setSocketPacketHelper(getSocketPacketHelper());
        setState(State.Connecting);
        getConnectionThread().start();
    }

    public void disconnect() {
        if (isDisconnected() || isDisconnecting()) {
            return;
        }

        setDisconnecting(true);

        getDisconnectionThread().start();
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

    public SocketClientAddress getConnectedAddress() {
        return getSocketConfigure().getAddress();
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
        SocketPacket packet = new SocketPacket(data);
        sendPacket(packet);
        return packet;
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
        SocketPacket packet = new SocketPacket(message);
        sendPacket(packet);
        return packet;
    }

    public SocketPacket sendPacket(final SocketPacket packet) {
        if (!isConnected()) {
            return null;
        }

        if (packet == null) {
            return null;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__enqueueNewPacket(packet);
            }
        }).start();

        return packet;
    }

    public void cancelSend(final SocketPacket packet) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getSendingPacketQueue()) {
                    if (self.getSendingPacketQueue().contains(packet)) {
                        self.getSendingPacketQueue().remove(packet);

                        self.__i__onSendPacketCancel(packet);
                    }
                }
            }
        }).start();
    }

    public SocketResponsePacket readDataToLength(final int length) {
        if (!isConnected()) {
            return null;
        }

        if (getSocketConfigure().getSocketPacketHelper().getReadStrategy() != SocketPacketHelper.ReadStrategy.Manually) {
            return null;
        }

        if (getReceivingResponsePacket() != null) {
            return null;
        }

        setReceivingResponsePacket(new SocketResponsePacket());

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!self.isConnected()) {
                    return;
                }

                self.__i__onReceivePacketBegin(self.getReceivingResponsePacket());
                try {
                    byte[] data = self.getSocketInputReader().readToLength(length);
                    self.getReceivingResponsePacket().setData(data);
                    if (self.getSocketConfigure().getCharsetName() != null) {
                        self.getReceivingResponsePacket().buildStringWithCharsetName(self.getSocketConfigure().getCharsetName());
                    }
                    self.__i__onReceivePacketEnd(self.getReceivingResponsePacket());
                    self.__i__onReceiveResponse(self.getReceivingResponsePacket());
                }
                catch (IOException e) {
                    e.printStackTrace();

                    if (self.getReceivingResponsePacket() != null) {
                        self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                        self.setReceivingResponsePacket(null);
                    }
                }
            }
        }).start();

        return getReceivingResponsePacket();
    }

    public SocketResponsePacket readDataToData(final byte[] data) {
        return readDataToData(data, true);
    }

    /**
     * 读取到与指定字节相同的字节序列后回调数据包
     *
     * @param data 指定字节序列
     * @param includeData 是否在回调的数据包中包含指定的字节序列
     * @return 将要读取的数据包实例
     */
    public SocketResponsePacket readDataToData(final byte[] data, final boolean includeData) {
        if (!isConnected()) {
            return null;
        }

        if (getSocketConfigure().getSocketPacketHelper().getReadStrategy() != SocketPacketHelper.ReadStrategy.Manually) {
            return null;
        }

        if (getReceivingResponsePacket() != null) {
            return null;
        }

        setReceivingResponsePacket(new SocketResponsePacket());

        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__onReceivePacketBegin(self.getReceivingResponsePacket());
                try {
                    byte[] result = self.getSocketInputReader().readToData(data, includeData);
                    self.getReceivingResponsePacket().setData(result);
                    if (self.getSocketConfigure().getCharsetName() != null) {
                        self.getReceivingResponsePacket().buildStringWithCharsetName(self.getSocketConfigure().getCharsetName());
                    }
                    self.__i__onReceivePacketEnd(self.getReceivingResponsePacket());
                    self.__i__onReceiveResponse(self.getReceivingResponsePacket());
                }
                catch (IOException e) {
                    e.printStackTrace();

                    if (self.getReceivingResponsePacket() != null) {
                        self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                        self.setReceivingResponsePacket(null);
                    }
                }
            }
        }).start();

        return getReceivingResponsePacket();
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
    public SocketClient registerSocketClientReceiveDelegate(SocketClientReceivingDelegate delegate) {
        if (!getSocketClientReceivingDelegates().contains(delegate)) {
            getSocketClientReceivingDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册信息接收回调
     * @param delegate 回调接收者
     */
    public SocketClient removeSocketClientReceiveDelegate(SocketClientReceivingDelegate delegate) {
        getSocketClientReceivingDelegates().remove(delegate);
        return this;
    }

    /* Properties */
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
     * 设置默认的编码格式
     * 为null表示不自动转换data到string
     */
    private String charsetName;
    public SocketClient setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        return this;
    }
    public String getCharsetName() {
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
            this.socketPacketHelper = new SocketPacketHelper();
        }
        return this.socketPacketHelper;
    }

    /**
     * 心跳包信息
     */
    private SocketHeartBeatHelper heartBeatHelper;
    public SocketClient setHeartBeatHelper(SocketHeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper;
        return this;
    }
    public SocketHeartBeatHelper getHeartBeatHelper() {
        if (this.heartBeatHelper == null) {
            this.heartBeatHelper = new SocketHeartBeatHelper();
        }
        return this.heartBeatHelper;
    }

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

    private SocketInputReader socketInputReader;
    protected SocketClient setSocketInputReader(SocketInputReader socketInputReader) {
        this.socketInputReader = socketInputReader;
        return this;
    }
    protected SocketInputReader getSocketInputReader() throws IOException {
        if (this.socketInputReader == null) {
            this.socketInputReader = new SocketInputReader(getRunningSocket().getInputStream());
        }
        return this.socketInputReader;
    }

    private SocketConfigure socketConfigure;
    protected SocketClient setSocketConfigure(SocketConfigure socketConfigure) {
        this.socketConfigure = socketConfigure;
        return this;
    }
    protected SocketConfigure getSocketConfigure() {
        if (this.socketConfigure == null) {
            this.socketConfigure = new SocketConfigure();
        }
        return this.socketConfigure;
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
    public enum State {
        Disconnected, Connecting, Connected
    }

    private boolean disconnecting;
    protected SocketClient setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
        return this;
    }
    public boolean isDisconnecting() {
        return this.disconnecting;
    }

    private LinkedBlockingQueue<SocketPacket> sendingPacketQueue;
    protected LinkedBlockingQueue<SocketPacket> getSendingPacketQueue() {
        if (sendingPacketQueue == null) {
            sendingPacketQueue = new LinkedBlockingQueue<SocketPacket>();
        }
        return sendingPacketQueue;
    }

    /**
     * 计时器
     */
    private CountDownTimer hearBeatCountDownTimer;
    protected CountDownTimer getHearBeatCountDownTimer() {
        if (this.hearBeatCountDownTimer == null) {
            this.hearBeatCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            self.__i__onTimeTick();
                        }
                    }).start();
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

    /**
     * 记录上次发送数据片段的时间
     * 仅在每个发送包开始发送时计时，结束后重置计时
     * NoSendingTime 表示当前没有在发送数据
     */
    private final static long NoSendingTime = -1;
    private long lastSendMessageTime = NoSendingTime;
    protected SocketClient setLastSendMessageTime(long lastSendMessageTime) {
        this.lastSendMessageTime = lastSendMessageTime;
        return this;
    }
    protected long getLastSendMessageTime() {
        return this.lastSendMessageTime;
    }

    private SocketPacket sendingPacket;
    protected SocketClient setSendingPacket(SocketPacket sendingPacket) {
        this.sendingPacket = sendingPacket;
        return this;
    }
    protected SocketPacket getSendingPacket() {
        return this.sendingPacket;
    }

    private SocketResponsePacket receivingResponsePacket;
    protected SocketClient setReceivingResponsePacket(SocketResponsePacket receivingResponsePacket) {
        this.receivingResponsePacket = receivingResponsePacket;
        return this;
    }
    protected SocketResponsePacket getReceivingResponsePacket() {
        return this.receivingResponsePacket;
    }

    private long lastReceiveProgressCallbackTime;
    protected SocketClient setLastReceiveProgressCallbackTime(long lastReceiveProgressCallbackTime) {
        this.lastReceiveProgressCallbackTime = lastReceiveProgressCallbackTime;
        return this;
    }
    protected long getLastReceiveProgressCallbackTime() {
        return this.lastReceiveProgressCallbackTime;
    }

    private ConnectionThread connectionThread;
    protected SocketClient setConnectionThread(ConnectionThread connectionThread) {
        this.connectionThread = connectionThread;
        return this;
    }
    protected ConnectionThread getConnectionThread() {
        if (this.connectionThread == null) {
            this.connectionThread = new ConnectionThread();
        }
        return this.connectionThread;
    }

    private DisconnectionThread disconnectionThread;
    protected SocketClient setDisconnectionThread(DisconnectionThread disconnectionThread) {
        this.disconnectionThread = disconnectionThread;
        return this;
    }
    protected DisconnectionThread getDisconnectionThread() {
        if (this.disconnectionThread == null) {
            this.disconnectionThread = new DisconnectionThread();
        }
        return this.disconnectionThread;
    }

    private SendThread sendThread;
    protected SocketClient setSendThread(SendThread sendThread) {
        this.sendThread = sendThread;
        return this;
    }
    protected SendThread getSendThread() {
        if (this.sendThread == null) {
            this.sendThread = new SendThread();
        }
        return this.sendThread;
    }

    private ReceiveThread receiveThread;
    protected SocketClient setReceiveThread(ReceiveThread receiveThread) {
        this.receiveThread = receiveThread;
        return this;
    }
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

    private ArrayList<SocketClientReceivingDelegate> socketClientReceivingDelegates;
    protected ArrayList<SocketClientReceivingDelegate> getSocketClientReceivingDelegates() {
        if (this.socketClientReceivingDelegates == null) {
            this.socketClientReceivingDelegates = new ArrayList<SocketClientReceivingDelegate>();
        }
        return this.socketClientReceivingDelegates;
    }

    private UIHandler uiHandler;
    protected UIHandler getUiHandler() {
        if (this.uiHandler == null) {
            this.uiHandler = new UIHandler(this);
        }
        return this.uiHandler;
    }
    private static class UIHandler extends Handler {
        private WeakReference<SocketClient> referenceSocketClient;

        public UIHandler(@NonNull SocketClient referenceSocketClient) {
            super(Looper.getMainLooper());

            this.referenceSocketClient = new WeakReference<SocketClient>(referenceSocketClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /* Overrides */
     
     
    /* Delegates */


    /* Protected Methods */
    @CallSuper
    protected void internalOnConnected() {
        setState(SocketClient.State.Connected);

        setLastSendHeartBeatMessageTime(System.currentTimeMillis());
        setLastReceiveMessageTime(System.currentTimeMillis());
        setLastSendMessageTime(NoSendingTime);

        setSendingPacket(null);
        setReceivingResponsePacket(null);

        __i__onConnected();
    }

    /* Private Methods */
    private void __i__enqueueNewPacket(final SocketPacket packet) {
        if (!isConnected()) {
            return;
        }

        synchronized (getSendingPacketQueue()) {
            try {
                getSendingPacketQueue().put(packet);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void __i__sendHeartBeat() {
        if (!isConnected()) {
            return;
        }

        if (getSocketConfigure() == null
                || getSocketConfigure().getHeartBeatHelper() == null
                || !getSocketConfigure().getHeartBeatHelper().isSendHeartBeatEnabled()) {
            return;
        }

        final SocketPacket packet = new SocketPacket(getSocketConfigure().getHeartBeatHelper().getSendData(), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                self.__i__enqueueNewPacket(packet);
            }
        }).start();
    }

    private void __i__onConnected() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onConnected();
                }
            });
            return;
        }

        ArrayList<SocketClientDelegate> delegatesCopy =
                (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onConnected(this);
        }

        getSendThread().start();
        getReceiveThread().start();
        getHearBeatCountDownTimer().start();
    }

    private void __i__onDisconnected() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onDisconnected();
                }
            });
            return;
        }

        ArrayList<SocketClientDelegate> delegatesCopy =
                (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
        int count = delegatesCopy.size();
        for (int i = 0; i < count; ++i) {
            delegatesCopy.get(i).onDisconnected(this);
        }
    }

    private void __i__onReceiveResponse(@NonNull final SocketResponsePacket responsePacket) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceiveResponse(responsePacket);
                }
            });
            return;
        }

        setLastReceiveMessageTime(System.currentTimeMillis());

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientDelegate> delegatesCopy =
                    (ArrayList<SocketClientDelegate>) getSocketClientDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onResponse(this, responsePacket);
            }
        }
    }

    private void __i__onSendPacketBegin(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketBegin(packet);
                }
            });
            return;
        }

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketBegin(this, packet);
            }
        }
    }

    private void __i__onSendPacketEnd(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketEnd(packet);
                }
            });
            return;
        }

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketEnd(this, packet);
            }
        }
    }

    private void __i__onSendPacketCancel(final SocketPacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendPacketCancel(packet);
                }
            });
            return;
        }

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendPacketCancel(this, packet);
            }
        }
    }

    private void __i__onSendingPacketInProgress(final SocketPacket packet, final int sendedLength, final int headerLength, final int packetLengthDataLength, final int dataLength, final int trailerLength) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSendingPacketInProgress(packet, sendedLength, headerLength, packetLengthDataLength, dataLength, trailerLength);
                }
            });
            return;
        }

        float progress = sendedLength / (float) (headerLength + packetLengthDataLength + dataLength + trailerLength);

        if (getSocketClientDelegates().size() > 0) {
            ArrayList<SocketClientSendingDelegate> delegatesCopy =
                    (ArrayList<SocketClientSendingDelegate>) getSocketClientSendingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onSendingPacketInProgress(this, packet, progress, sendedLength);
            }
        }
    }

    private void __i__onReceivePacketBegin(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketBegin(packet);
                }
            });
            return;
        }

        if (getSocketClientReceivingDelegates().size() > 0) {
            ArrayList<SocketClientReceivingDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceivingDelegate>) getSocketClientReceivingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onReceivePacketBegin(this, packet);
            }
        }
    }

    private void __i__onReceivePacketEnd(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketEnd(packet);
                }
            });
            return;
        }

        if (getSocketClientReceivingDelegates().size() > 0) {
            ArrayList<SocketClientReceivingDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceivingDelegate>) getSocketClientReceivingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onReceivePacketEnd(this, packet);
            }
        }
    }

    private void __i__onReceivePacketCancel(final SocketResponsePacket packet) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivePacketCancel(packet);
                }
            });
            return;
        }

        if (getSocketClientReceivingDelegates().size() > 0) {
            ArrayList<SocketClientReceivingDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceivingDelegate>) getSocketClientReceivingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onReceivePacketCancel(this, packet);
            }
        }
    }

    private void __i__onReceivingPacketInProgress(final SocketResponsePacket packet, final int receivedLength, final int headerLength, final int packetLengthDataLength, final int dataLength, final int trailerLength) {

        long currentTime = System.currentTimeMillis();
        if (currentTime - getLastReceiveProgressCallbackTime() < (1000 / 24)) {
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onReceivingPacketInProgress(packet, receivedLength, headerLength, packetLengthDataLength, dataLength, trailerLength);
                }
            });
            return;
        }

        float progress = receivedLength / (float) (headerLength + packetLengthDataLength + dataLength + trailerLength);

        if (getSocketClientReceivingDelegates().size() > 0) {
            ArrayList<SocketClientReceivingDelegate> delegatesCopy =
                    (ArrayList<SocketClientReceivingDelegate>) getSocketClientReceivingDelegates().clone();
            int count = delegatesCopy.size();
            for (int i = 0; i < count; ++i) {
                delegatesCopy.get(i).onReceivingPacketInProgress(this, packet, progress, receivedLength);
            }
        }

        setLastReceiveProgressCallbackTime(System.currentTimeMillis());
    }

    private void __i__onTimeTick() {
        if (!isConnected()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (getSocketConfigure().getHeartBeatHelper().isSendHeartBeatEnabled()) {
            if (currentTime - getLastSendHeartBeatMessageTime() >= getSocketConfigure().getHeartBeatHelper().getHeartBeatInterval()) {
                __i__sendHeartBeat();
                setLastSendHeartBeatMessageTime(currentTime);
            }
        }

        if (getSocketConfigure().getSocketPacketHelper().isReceiveTimeoutEnabled()) {
            if (currentTime - getLastReceiveMessageTime() >= getSocketConfigure().getSocketPacketHelper().getReceiveTimeout()) {
                disconnect();
            }
        }

        if (getSocketConfigure().getSocketPacketHelper().isSendTimeoutEnabled()
                && getLastSendMessageTime() != NoSendingTime) {
            if (currentTime - getLastSendMessageTime() >= getSocketConfigure().getSocketPacketHelper().getSendTimeout()) {
                disconnect();
            }
        }
    }
     
    /* Enums */

    /* Inner Classes */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            try {
                SocketClientAddress address = self.getSocketConfigure().getAddress();

                if (Thread.interrupted()) {
                    return;
                }

                self.getRunningSocket().connect(address.getInetSocketAddress(), address.getConnectionTimeout());

                if (Thread.interrupted()) {
                    return;
                }

                self.setState(SocketClient.State.Connected);

                self.setLastSendHeartBeatMessageTime(System.currentTimeMillis());
                self.setLastReceiveMessageTime(System.currentTimeMillis());
                self.setLastSendMessageTime(NoSendingTime);

                self.setSendingPacket(null);
                self.setReceivingResponsePacket(null);

                self.setConnectionThread(null);

                self.__i__onConnected();
            }
            catch (IOException e) {
                e.printStackTrace();

                self.disconnect();
            }
        }
    }

    private class DisconnectionThread extends Thread {
        @Override
        public void run() {
            super.run();

            if (self.connectionThread != null) {
                self.getConnectionThread().interrupt();
                self.setConnectionThread(null);
            }

            if (!self.getRunningSocket().isClosed()
                || self.isConnecting()) {
                try {
                    self.getRunningSocket().getOutputStream().close();
                    self.getRunningSocket().getInputStream().close();
                }
                catch (IOException e) {
//                e.printStackTrace();
                }
                finally {
                    try {
                        self.getRunningSocket().close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    self.setRunningSocket(null);
                }
            }

            if (self.sendThread != null) {
                self.getSendThread().interrupt();
                self.setSendThread(null);
            }
            if (self.receiveThread != null) {
                self.getReceiveThread().interrupt();
                self.setReceiveThread(null);
            }

            self.setDisconnecting(false);
            self.setState(SocketClient.State.Disconnected);
            self.setSocketInputReader(null);
            self.setSocketConfigure(null);

            if (self.hearBeatCountDownTimer != null) {
                self.hearBeatCountDownTimer.cancel();
            }

            if (self.getSendingPacket() != null) {
                self.__i__onSendPacketCancel(self.getSendingPacket());
                self.setSendingPacket(null);
            }

            SocketPacket packet;
            while ((packet = self.getSendingPacketQueue().poll()) != null) {
                self.__i__onSendPacketCancel(packet);
            }

            if (self.getReceivingResponsePacket() != null) {
                self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                self.setReceivingResponsePacket(null);
            }

            self.setDisconnectionThread(null);

            self.__i__onDisconnected();
        }
    }

    private class SendThread extends Thread {
        public SendThread() {
        }

        @Override
        public void run() {
            super.run();

            SocketPacket packet;
            try {
                while (self.isConnected()
                       && !Thread.interrupted()
                        && (packet = self.getSendingPacketQueue().take()) != null) {
                    self.setSendingPacket(packet);
                    self.setLastSendMessageTime(System.currentTimeMillis());

                    if (packet.getData() == null
                        && packet.getMessage() != null) {
                        if (self.getSocketConfigure().getCharsetName() == null) {
                            throw new IllegalArgumentException("we need string charset to send string type message");
                        }
                        else {
                            packet.buildDataWithCharsetName(self.getSocketConfigure().getCharsetName());
                        }
                    }

                    if (packet.getData() == null) {
                        self.__i__onSendPacketCancel(packet);
                        self.setSendingPacket(null);
                        continue;
                    }

                    byte[] headerData = self.getSocketConfigure().getSocketPacketHelper().getSendHeaderData();
                    int headerDataLength = headerData == null ? 0 : headerData.length;

                    byte[] trailerData = self.getSocketConfigure().getSocketPacketHelper().getSendTrailerData();
                    int trailerDataLength = trailerData == null ? 0 : trailerData.length;

                    byte[] packetLengthData = self.getSocketConfigure().getSocketPacketHelper().getSendPacketLengthData(packet.getData().length + trailerDataLength);
                    int packetLengthDataLength = packetLengthData == null ? 0 : packetLengthData.length;

                    int sendedPacketLength = 0;

                    packet.setHeaderData(headerData);
                    packet.setTrailerData(trailerData);
                    packet.setPacketLengthData(packetLengthData);

                    if (headerDataLength + packetLengthDataLength + packet.getData().length + trailerDataLength <= 0) {
                        self.__i__onSendPacketCancel(packet);
                        self.setSendingPacket(null);
                        continue;
                    }

                    self.__i__onSendPacketBegin(packet);
                    self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);

                    try {
                        if (headerDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(headerData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += headerDataLength;
                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        if (packetLengthDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(packetLengthData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += packetLengthDataLength;
                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        if (packet.getData().length > 0) {
                            int segmentLength = self.getRunningSocket().getSendBufferSize();
                            if (self.getSocketConfigure().getSocketPacketHelper().isSendSegmentEnabled()) {
                                segmentLength = Math.min(segmentLength, self.getSocketConfigure().getSocketPacketHelper().getSendSegmentLength());
                            }

                            int offset = 0;

                            while (offset < packet.getData().length) {
                                int end = offset + segmentLength;
                                end = Math.min(end, packet.getData().length);
                                self.getRunningSocket().getOutputStream().write(packet.getData(), offset, end - offset);
                                self.getRunningSocket().getOutputStream().flush();
                                self.setLastSendMessageTime(System.currentTimeMillis());

                                sendedPacketLength += end - offset;

                                self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);

                                offset = end;
                            }
                        }

                        if (trailerDataLength > 0) {
                            self.getRunningSocket().getOutputStream().write(trailerData);
                            self.getRunningSocket().getOutputStream().flush();
                            self.setLastSendMessageTime(System.currentTimeMillis());

                            sendedPacketLength += trailerDataLength;

                            self.__i__onSendingPacketInProgress(packet, sendedPacketLength, headerDataLength, packetLengthDataLength, packet.getData().length, trailerDataLength);
                        }

                        self.__i__onSendPacketEnd(packet);
                        self.setSendingPacket(null);

                        self.setLastSendMessageTime(NoSendingTime);
                    }
                    catch (IOException e) {
                        e.printStackTrace();

                        if (self.getSendingPacket() != null) {
                            self.__i__onSendPacketCancel(self.getSendingPacket());
                            self.setSendingPacket(null);
                        }
                    }
                }
            }
            catch (InterruptedException e) {
//                e.printStackTrace();
                if (self.getSendingPacket() != null) {
                    self.__i__onSendPacketCancel(self.getSendingPacket());
                    self.setSendingPacket(null);
                }
            }
        }
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();

            if (self.getSocketConfigure().getSocketPacketHelper().getReadStrategy() == SocketPacketHelper.ReadStrategy.Manually) {
                return;
            }

            try {
                while (self.isConnected()
                       && self.getSocketInputReader() != null
                       && !Thread.interrupted()) {
                    SocketResponsePacket packet = new SocketResponsePacket();
                    self.setReceivingResponsePacket(packet);

                    byte[] headerData = self.getSocketConfigure().getSocketPacketHelper().getReceiveHeaderData();
                    int headerDataLength = headerData == null ? 0 : headerData.length;

                    byte[] trailerData = self.getSocketConfigure().getSocketPacketHelper().getReceiveTrailerData();
                    int trailerDataLength = trailerData == null ? 0 : trailerData.length;

                    int packetLengthDataLength = self.getSocketConfigure().getSocketPacketHelper().getReceivePacketLengthDataLength();

                    int dataLength = 0;
                    int receivedPacketLength = 0;

                    self.__i__onReceivePacketBegin(packet);

                    if (headerDataLength > 0) {
                        byte[] data = self.getSocketInputReader().readToData(headerData, true);
                        self.setLastReceiveMessageTime(System.currentTimeMillis());
                        packet.setHeaderData(data);

                        receivedPacketLength += headerDataLength;
                    }

                    if (self.getSocketConfigure().getSocketPacketHelper().getReadStrategy() == SocketPacketHelper.ReadStrategy.AutoReadByLength) {
                        if (packetLengthDataLength < 0) {
                            self.__i__onReceivePacketCancel(packet);
                            self.setReceivingResponsePacket(null);
                        }
                        else if (packetLengthDataLength == 0) {
                            self.__i__onReceivePacketEnd(packet);
                            self.setReceivingResponsePacket(null);
                        }

                        byte[] data = self.getSocketInputReader().readToLength(packetLengthDataLength);
                        self.setLastReceiveMessageTime(System.currentTimeMillis());
                        packet.setPacketLengthData(data);

                        receivedPacketLength += packetLengthDataLength;

                        int bodyTrailerLength = self.getSocketConfigure().getSocketPacketHelper().getReceivePacketDataLength(data);

                        dataLength = bodyTrailerLength - trailerDataLength;

                        if (dataLength > 0) {
                            int segmentLength = self.getRunningSocket().getReceiveBufferSize();
                            if (self.getSocketConfigure().getSocketPacketHelper().isReceiveSegmentEnabled()) {
                                segmentLength = Math.min(segmentLength, self.getSocketConfigure().getSocketPacketHelper().getReceiveSegmentLength());
                            }
                            int offset = 0;
                            while (offset < dataLength) {
                                int end = offset + segmentLength;
                                end = Math.min(end, dataLength);
                                data = self.getSocketInputReader().readToLength(end - offset);
                                self.setLastReceiveMessageTime(System.currentTimeMillis());

                                if (packet.getData() == null) {
                                    packet.setData(data);
                                }
                                else {
                                    byte[] mergedData = new byte[packet.getData().length + data.length];

                                    System.arraycopy(packet.getData(), 0, mergedData, 0, packet.getData().length);
                                    System.arraycopy(data, 0, mergedData, packet.getData().length, data.length);

                                    packet.setData(mergedData);
                                }

                                receivedPacketLength += end - offset;

                                self.__i__onReceivingPacketInProgress(packet, receivedPacketLength, headerDataLength, packetLengthDataLength, dataLength, trailerDataLength);

                                offset = end;
                            }
                        }
                        else if (dataLength < 0) {
                            self.__i__onReceivePacketCancel(packet);
                            self.setReceivingResponsePacket(null);
                        }

                        if (trailerDataLength > 0) {
                            data = self.getSocketInputReader().readToLength(trailerDataLength);
                            self.setLastReceiveMessageTime(System.currentTimeMillis());
                            packet.setTrailerData(data);

                            receivedPacketLength += trailerDataLength;

                            self.__i__onReceivingPacketInProgress(packet, receivedPacketLength, headerDataLength, packetLengthDataLength, dataLength, trailerDataLength);
                        }
                    }
                    else if (self.getSocketConfigure().getSocketPacketHelper().getReadStrategy() == SocketPacketHelper.ReadStrategy.AutoReadToTrailer) {
                        if (trailerDataLength > 0) {
                            byte[] data = self.getSocketInputReader().readToData(trailerData, false);
                            self.setLastReceiveMessageTime(System.currentTimeMillis());
                            packet.setData(data);
                            packet.setTrailerData(trailerData);

                            receivedPacketLength += data.length;
                        }
                        else {
                            self.__i__onReceivePacketCancel(packet);
                            self.setReceivingResponsePacket(null);
                        }
                    }

                    packet.setHeartBeat(self.getSocketConfigure().getHeartBeatHelper().isReceiveHeartBeatPacket(packet));

                    if (self.getSocketConfigure().getCharsetName() != null) {
                        packet.buildStringWithCharsetName(self.getSocketConfigure().getCharsetName());
                    }

                    self.__i__onReceivePacketEnd(packet);
                    self.__i__onReceiveResponse(packet);
                    self.setReceivingResponsePacket(null);
                }
            }
            catch (Exception e) {
//                e.printStackTrace();
                self.disconnect();

                if (self.getReceivingResponsePacket() != null) {
                    self.__i__onReceivePacketCancel(self.getReceivingResponsePacket());
                    self.setReceivingResponsePacket(null);
                }
            }
        }
    }

}