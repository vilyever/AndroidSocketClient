package com.vilyever.socketclient.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.vilyever.socketclient.PollingHelper;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketPacket;
import com.vilyever.socketclient.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetNames;
import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.StringValidation;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * SocketServer
 * AndroidSocketClient <com.vilyever.socketclient.server>
 * Created by vilyever on 2016/3/18.
 * Feature:
 */
public class SocketServer implements SocketClient.SocketDelegate {
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
     * 统一配置是否支持按行读取消息
     * 若否则读取每一次缓冲返回一次消息
     * 即受到的消息末尾是 '\r\n' 符号
     * 此操作可以解决发送方发送过快时缓冲池内存有多条信息
     */
    private boolean supportReadLine = true;
    public SocketServer setSupportReadLine(boolean supportReadLine) {
        this.supportReadLine = supportReadLine;
        return this;
    }
    public boolean isSupportReadLine() {
        return this.supportReadLine;
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
            this.charsetName = CharsetNames.UTF_8;
        }
        return this.charsetName;
    }

    /**
     * 统一配置心跳包信息
     */
    private byte[] heartBeatMessage;
    public SocketServer setHeartBeatMessage(String heartBeatMessage) {
        return setHeartBeatMessage(heartBeatMessage, getCharsetName());
    }
    public SocketServer setHeartBeatMessage(String heartBeatMessage, String charsetName) {
        return setHeartBeatMessage(heartBeatMessage.getBytes(Charset.forName(charsetName)));
    }
    public SocketServer setHeartBeatMessage(byte[] heartBeatMessage) {
        this.heartBeatMessage = heartBeatMessage;
        return this;
    }
    public byte[] getHeartBeatMessage() {
        if (this.heartBeatMessage == null) {
            this.heartBeatMessage = SocketPacket.DefaultHeartBeatMessage;
        }
        return this.heartBeatMessage;
    }

    /**
     * 统一配置心跳包发送间隔
     */
    private long heartBeatInterval = SocketServerClient.DefaultHeartBeatInterval;
    public SocketServer setHeartBeatInterval(long heartBeatInterval) {
        if (heartBeatInterval < 0) {
            throw new IllegalArgumentException("we need heartBeatInterval > 0");
        }
        this.heartBeatInterval = heartBeatInterval;

        ArrayList<SocketServerClient> copyList =
                (ArrayList<SocketServerClient>) getRunningSocketServerClients().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).setHeartBeatInterval(heartBeatInterval);
        }

        return this;
    }
    public long getHeartBeatInterval() {
        return this.heartBeatInterval;
    }

    /**
     * 统一配置超时无消息断开间隔
     */
    private long remoteNoReplyAliveTimeout = SocketServerClient.DefaultRemoteNoReplyAliveTimeout;
    public SocketServer setRemoteNoReplyAliveTimeout(long remoteNoReplyAliveTimeout) {
        if (remoteNoReplyAliveTimeout < 0) {
            throw new IllegalArgumentException("we need remoteNoReplyAliveTimeout > 0");
        }
        this.remoteNoReplyAliveTimeout = remoteNoReplyAliveTimeout;

        ArrayList<SocketServerClient> copyList =
                (ArrayList<SocketServerClient>) getRunningSocketServerClients().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).setRemoteNoReplyAliveTimeout(remoteNoReplyAliveTimeout);
        }

        return this;
    }
    public long getRemoteNoReplyAliveTimeout() {
        return this.remoteNoReplyAliveTimeout;
    }

    /**
     * 统一配置自动应答
     */
    private PollingHelper pollingHelper;
    public PollingHelper getPollingHelper() {
        if (this.pollingHelper == null) {
            this.pollingHelper = new PollingHelper(getCharsetName());
        }
        return this.pollingHelper;
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

    public interface SocketServerDelegate {
        void onServerBeginListen(SocketServer socketServer, int port);
        void onServerStopListen(SocketServer socketServer, int port);
        void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient);
        void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient);

        class SimpleSocketServerDelegate implements SocketServerDelegate {
            @Override
            public void onServerBeginListen(SocketServer socketServer, int port) {

            }

            @Override
            public void onServerStopListen(SocketServer socketServer, int port) {

            }

            @Override
            public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {

            }

            @Override
            public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {

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
                    this.referenceSocketServer.get().onSocketServerStopListen();
                    break;
                case ClientConnected:
                    this.referenceSocketServer.get().onSocketServerClientConnected((SocketServerClient) msg.obj);
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
    /** {@link SocketClient.SocketDelegate} */
    @Override
    public void onConnected(SocketClient client) {

    }

    @Override
    public void onDisconnected(SocketClient client) {
        onSocketServerClientDisconnected((SocketServerClient) client);
    }

    @Override
    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

    }


    /* Protected Methods */
    @WorkerThread
    protected SocketServerClient getSocketServerClient(Socket socket) {
        return new SocketServerClient(socket);
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
    protected void onSocketServerStopListen() {
        setListening(false);
        this.listenThread = null;
        this.runningServerSocket = null;

        disconnectAllClients();

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onServerStopListen(this, getPort());
        }
    }

    @CallSuper
    protected void onSocketServerClientConnected(SocketServerClient socketServerClient) {
        getRunningSocketServerClients().add(socketServerClient);

        socketServerClient.registerSocketDelegate(this);

        socketServerClient.setSupportReadLine(isSupportReadLine());
        socketServerClient.setCharsetName(getCharsetName());
        socketServerClient.setHeartBeatMessage(getHeartBeatMessage());
        socketServerClient.setHeartBeatInterval(getHeartBeatInterval());
        socketServerClient.setRemoteNoReplyAliveTimeout(getRemoteNoReplyAliveTimeout());
        socketServerClient.getPollingHelper().append(getPollingHelper());

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientConnected(this, socketServerClient);
        }
    }

    @CallSuper
    protected void onSocketServerClientDisconnected(SocketServerClient socketServerClient) {

        self.getRunningSocketServerClients().remove(socketServerClient);

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientDisconnected(this, socketServerClient);
        }
    }

    /* Private Methods */
    private boolean checkServerSocketAvailable() {
        return getRunningServerSocket() != null && !getRunningServerSocket().isClosed();
    }

    private void disconnectAllClients() {
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
            while (!Thread.interrupted() && self.checkServerSocketAvailable()) {
                Socket socket = null;
                try {
                    socket = self.getRunningServerSocket().accept();

                    SocketServerClient socketServerClient = self.getSocketServerClient(socket);

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