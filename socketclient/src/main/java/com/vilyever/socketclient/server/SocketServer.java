package com.vilyever.socketclient.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.HeartBeatHelper;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketConfigure;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;
import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.StringValidation;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * SocketServer
 * AndroidSocketClient <com.vilyever.socketclient.server>
 * Created by vilyever on 2016/3/18.
 * Feature:
 */
public class SocketServer implements SocketClientDelegate {
    final SocketServer self = this;

    public static final int NoPort = -1;
    public static final int MaxPort = 65535;

    
    /* Constructors */
    public SocketServer() {
    }

    /* Public Methods */
    public boolean beginListen(int port) {
        if (isListening()) {
            return false;
        }

        setPort(port);
        if (getRunningServerSocket() == null) {
            return false;
        }

        getSocketConfigure().setCharsetName(getCharsetName()).setHeartBeatHelper(getHeartBeatHelper()).setSocketPacketHelper(getSocketPacketHelper());

        onSocketServerBeginListen();
        getListenThread().start();

        return true;
    }

    public int beginListenFromPort(int port) {
        if (isListening()) {
            return NoPort;
        }

        while (port <= MaxPort) {
            if (beginListen(port)) {
                return port;
            }
            port++;
        }

        return NoPort;
    }

    public void stopListen() {
        if (isListening()) {
            getListenThread().interrupt();
            try {
                getRunningServerSocket().close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getIP() {
        return getRunningServerSocket().getLocalSocketAddress().toString().substring(1);
    }

    /**
     * 注册监听回调
     * @param delegate 回调接收者
     */
    public SocketServer registerSocketServerDelegate(SocketServerDelegate delegate) {
        if (!getSocketServerDelegates().contains(delegate)) {
            getSocketServerDelegates().add(delegate);
        }
        return this;
    }

    /**
     * 取消注册监听回调
     * @param delegate 回调接收者
     */
    public SocketServer removeSocketServerDelegate(SocketServerDelegate delegate) {
        getSocketServerDelegates().remove(delegate);
        return this;
    }

    /* Properties */
    private ServerSocket runningServerSocket;
    protected ServerSocket getRunningServerSocket() {
        if (this.runningServerSocket == null) {
            try {
                this.runningServerSocket = new ServerSocket(getPort());
            }
            catch (IOException e) {
//                e.printStackTrace();
            }
        }
        return this.runningServerSocket;
    }

    private int port = NoPort;
    public int getPort() {
        return this.port;
    }
    protected SocketServer setPort(int port) {
        if (!StringValidation.validateRegex(String.format("%d", port), StringValidation.RegexPort)) {
            ExceptionThrower.throwIllegalStateException("we need a correct remote port to listen");
        }

        if (isListening()) {
            return this;
        }

        this.port = port;
        return this;
    }
    
    private boolean listening;
    protected SocketServer setListening(boolean listening) {
        this.listening = listening;
        return this; 
    }
    public boolean isListening() {
        return this.listening;
    }

    private ListenThread listenThread;
    protected ListenThread getListenThread() {
        if (this.listenThread == null) {
            this.listenThread = new ListenThread();
        }
        return this.listenThread;
    }

    /**
     * 统一配置默认的编码格式
     */
    private String charsetName;
    public SocketServer setCharsetName(String charsetName) {
        this.charsetName = charsetName;
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
    public SocketServer setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
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
    public SocketServer setHeartBeatHelper(HeartBeatHelper heartBeatHelper) {
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

    private ArrayList<SocketServerClient> runningSocketServerClients;
    protected ArrayList<SocketServerClient> getRunningSocketServerClients() {
        if (this.runningSocketServerClients == null) {
            this.runningSocketServerClients = new ArrayList<SocketServerClient>();
        }
        return this.runningSocketServerClients;
    }

    private ArrayList<SocketServerDelegate> socketServerDelegates;
    protected ArrayList<SocketServerDelegate> getSocketServerDelegates() {
        if (this.socketServerDelegates == null) {
            this.socketServerDelegates = new ArrayList<SocketServerDelegate>();
        }
        return this.socketServerDelegates;
    }

    private UIHandler uiHandler;
    protected UIHandler getUiHandler() {
        if (this.uiHandler == null) {
            this.uiHandler = new UIHandler(this);
        }
        return this.uiHandler;
    }
    protected static class UIHandler extends Handler {
        private WeakReference<SocketServer> referenceSocketServer;

        public UIHandler(@NonNull SocketServer referenceSocketClient) {
            super(Looper.getMainLooper());

            this.referenceSocketServer = new WeakReference<SocketServer>(referenceSocketClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (MessageType.typeFromWhat(msg.what)) {
                case StopListen:
                    this.referenceSocketServer.get().internalOnSocketServerStopListen();
                    break;
                case ClientConnected:
                    this.referenceSocketServer.get().internalOnSocketServerClientConnected((SocketServerClient) msg.obj);
                    break;
            }
        }

        public enum MessageType {
            StopListen, ClientConnected;

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
    @Override
    public void onConnected(SocketClient client) {

    }

    @Override
    public void onDisconnected(SocketClient client) {
        internalOnSocketServerClientDisconnected((SocketServerClient) client);
    }

    @Override
    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

    }


    /* Protected Methods */
    @WorkerThread
    protected SocketServerClient internalGetSocketServerClient(Socket socket) {
        return new SocketServerClient(socket, getSocketConfigure());
    }

    @CallSuper
    protected void onSocketServerBeginListen() {
        setListening(true);

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onServerBeginListen(this, getPort());
        }
    }

    @CallSuper
    protected void internalOnSocketServerStopListen() {
        setListening(false);
        this.listenThread = null;
        this.runningServerSocket = null;

        internalDisconnectAllClients();

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onServerStopListen(this, getPort());
        }
    }

    @CallSuper
    protected void internalOnSocketServerClientConnected(SocketServerClient socketServerClient) {
        getRunningSocketServerClients().add(socketServerClient);

        socketServerClient.registerSocketClientDelegate(this);

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientConnected(this, socketServerClient);
        }
    }

    @CallSuper
    protected void internalOnSocketServerClientDisconnected(SocketServerClient socketServerClient) {
        getRunningSocketServerClients().remove(socketServerClient);

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientDisconnected(this, socketServerClient);
        }
    }

    /* Private Methods */
    private boolean internalCheckServerSocketAvailable() {
        return getRunningServerSocket() != null && !getRunningServerSocket().isClosed();
    }

    private void internalDisconnectAllClients() {
        while (getRunningSocketServerClients().size() > 0) {
            SocketServerClient client = getRunningSocketServerClients().get(0);
            getRunningSocketServerClients().remove(client);
            client.disconnect();
        }
    }

    /* Inner Classes */
    private class ListenThread extends Thread  {
        private boolean running;
        protected ListenThread setRunning(boolean running) {
            this.running = running;
            return this;
        }
        protected boolean isRunning() {
            return this.running;
        }

        @Override
        public void run() {
            super.run();
            setRunning(true);
            while (!Thread.interrupted() && self.internalCheckServerSocketAvailable()) {
                Socket socket = null;
                try {
                    socket = self.getRunningServerSocket().accept();

                    SocketServerClient socketServerClient = self.internalGetSocketServerClient(socket);

                    Message message = Message.obtain();
                    message.what = UIHandler.MessageType.ClientConnected.what();
                    message.obj = socketServerClient;
                    self.getUiHandler().sendMessage(message);
                }
                catch (IOException e) {
//                    e.printStackTrace();
                }
            }

            setRunning(false);

            Message message = Message.obtain();
            message.what = UIHandler.MessageType.StopListen.what();
            self.getUiHandler().sendMessage(message);
        }
    }
}