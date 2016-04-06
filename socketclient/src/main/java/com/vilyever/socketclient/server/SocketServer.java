package com.vilyever.socketclient.server;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketPacket;
import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.StringValidation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SocketServer
 * AndroidSocketClient <com.vilyever.socketclient.server>
 * Created by vilyever on 2016/3/18.
 * Feature:
 */
public class SocketServer implements SocketClient.SocketDelegate {
    final SocketServer self = this;

    public static final int NoPort = -1;

    
    /* Constructors */
    public SocketServer() {
    }

    /* Public Methods */
    public boolean beginListen(int port) {
        if (getListenThread().isAlive() && !getListenThread().isInterrupted()) {
            return false;
        }

        setPort(port);
        if (getRunningServerSocket() == null) {
            return false;
        }

        getListenThread().start();
        return true;
    }

    public int beginListenFromPort(int port) {
        if (getListenThread().isAlive() && !getListenThread().isInterrupted()) {
            return NoPort;
        }

        while (port < 65536) {
            setPort(port);
            if (getRunningServerSocket() != null) {
                beginListen(port);
                return port;
            }
            port++;
        }

        return NoPort;
    }

    public void stopListen() {
        if (getListenThread().isAlive() && !getListenThread().isInterrupted()) {
            getListenThread().interrupt();
        }
    }

    public String getIP() {
        return getRunningServerSocket().getLocalSocketAddress().toString().substring(1);
    }

    public SocketServer registerQueryResponse(String query, String response) {
        getQueryResponseMap().put(query, response);
        return this;
    }

    public SocketServer registerQueryResponse(HashMap<String, String> queryResponseMap) {
        getQueryResponseMap().putAll(queryResponseMap);
        return this;
    }

    public SocketServer removeQueryResponse(String query) {
        getQueryResponseMap().remove(query);
        return this;
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
        this.port = port;
        return this;
    }

    private ListenThread listenThread;
    protected ListenThread getListenThread() {
        if (this.listenThread == null
            || this.listenThread.isInterrupted()
            || !this.listenThread.isAlive()) {
            this.listenThread = new ListenThread();
        }
        return this.listenThread;
    }

    /**
     * 统一配置心跳包信息
     */
    private String heartBeatMessage;
    public SocketServer setHeartBeatMessage(String heartBeatMessage) {
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
     * 统一配置自动应答键值对
     */
    private HashMap<String, String> queryResponseMap;
    protected HashMap<String, String> getQueryResponseMap() {
        if (this.queryResponseMap == null) {
            this.queryResponseMap = new HashMap<String, String>();
        }
        return this.queryResponseMap;
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
        void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient);
        void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient);

        class SimpleSocketServerDelegate implements SocketServerDelegate {
            @Override
            public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {

            }

            @Override
            public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {

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
    public void onResponse(SocketClient client, @NonNull String response) {

    }


    /* Protected Methods */
    protected SocketServerClient getSocketServerClient(Socket socket) {
        return new SocketServerClient(socket);
    }

    @CallSuper
    protected void onSocketServerClientConnected(SocketServerClient socketServerClient) {

        socketServerClient.setHeartBeatMessage(getHeartBeatMessage());
        socketServerClient.setHeartBeatInterval(getHeartBeatInterval());
        socketServerClient.setRemoteNoReplyAliveTimeout(getRemoteNoReplyAliveTimeout());
        socketServerClient.registerQueryResponse(getQueryResponseMap());

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


    /* Inner Classes */
    private class ListenThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                Socket socket = null;
                try {
                    socket = self.getRunningServerSocket().accept();

                    if (isInterrupted()) {
                        return;
                    }

                    SocketServerClient socketServerClient = self.getSocketServerClient(socket);

                    socketServerClient.registerSocketDelegate(self);

                    self.getRunningSocketServerClients().add(socketServerClient);

                    self.onSocketServerClientConnected(socketServerClient);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
    
        }
    }
}