package com.vilyever.socketclient.server;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketConfigure;
import com.vilyever.socketclient.helper.SocketHeartBeatHelper;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.IPUtil;
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

        getSocketConfigure().setCharsetName(getCharsetName()).setAddress(new SocketClientAddress(IPUtil.getLocalIPAddress(true), "" + port)).setHeartBeatHelper(getHeartBeatHelper()).setSocketPacketHelper(getSocketPacketHelper());

        if (getRunningServerSocket() == null) {
            return false;
        }


        setListening(true);
        __i__onSocketServerBeginListen();

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

    public SocketClientAddress getListeningAddress() {
        return getSocketConfigure().getAddress();
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
    protected SocketServer setRunningServerSocket(ServerSocket runningServerSocket) {
        this.runningServerSocket = runningServerSocket;
        return this;
    }
    protected ServerSocket getRunningServerSocket() {
        if (this.runningServerSocket == null) {
            try {
                this.runningServerSocket = new ServerSocket(getPort());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.runningServerSocket;
    }

    private int port = NoPort;
    public int getPort() {
        return this.port;
    }
    protected SocketServer setPort(int port) {
        if (!StringValidation.validateRegex("" + port, StringValidation.RegexPort)) {
            throw new IllegalArgumentException("we need a correct remote port to listen");
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
    protected SocketServer setListenThread(ListenThread listenThread) {
        this.listenThread = listenThread;
        return this;
    }
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
        return this.charsetName;
    }

    private SocketPacketHelper socketPacketHelper;
    public SocketServer setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
        this.socketPacketHelper = socketPacketHelper;
        return this;
    }
    public SocketPacketHelper getSocketPacketHelper() {
        if (this.socketPacketHelper == null) {
            this.socketPacketHelper = new SocketPacketHelper();
        }
        return this.socketPacketHelper;
    }

    private SocketHeartBeatHelper heartBeatHelper;
    public SocketServer setHeartBeatHelper(SocketHeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper;
        return this;
    }
    public SocketHeartBeatHelper getHeartBeatHelper() {
        if (this.heartBeatHelper == null) {
            this.heartBeatHelper = new SocketHeartBeatHelper();
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
    private static class UIHandler extends Handler {
        private WeakReference<SocketServer> referenceSocketServer;

        public UIHandler(@NonNull SocketServer referenceSocketServer) {
            super(Looper.getMainLooper());

            this.referenceSocketServer = new WeakReference<SocketServer>(referenceSocketServer);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /* Overrides */
     
     
    /* Delegates */
    @Override
    public void onConnected(SocketClient client) {

    }

    @Override
    public void onDisconnected(SocketClient client) {
        getRunningSocketServerClients().remove(client);
        __i__onSocketServerClientDisconnected((SocketServerClient) client);
    }

    @Override
    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

    }


    /* Protected Methods */
    @WorkerThread
    protected SocketServerClient internalGetSocketServerClient(Socket socket) {
        return new SocketServerClient(socket, getSocketConfigure());
    }

    /* Private Methods */
    private boolean __i__checkServerSocketAvailable() {
        return getRunningServerSocket() != null && !getRunningServerSocket().isClosed();
    }

    private void __i__disconnectAllClients() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__disconnectAllClients();
                }
            });
            return;
        }

        while (getRunningSocketServerClients().size() > 0) {
            SocketServerClient client = getRunningSocketServerClients().get(0);
            getRunningSocketServerClients().remove(client);
            client.disconnect();
        }
    }

    private void __i__onSocketServerBeginListen() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSocketServerBeginListen();
                }
            });
            return;
        }

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onServerBeginListen(this, getPort());
        }

        getListenThread().start();
    }

    private void __i__onSocketServerStopListen() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSocketServerStopListen();
                }
            });
            return;
        }

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onServerStopListen(this, getPort());
        }
    }

    private void __i__onSocketServerClientConnected(final SocketServerClient socketServerClient) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSocketServerClientConnected(socketServerClient);
                }
            });
            return;
        }

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientConnected(this, socketServerClient);
        }
    }

    private void __i__onSocketServerClientDisconnected(final SocketServerClient socketServerClient) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    self.__i__onSocketServerClientDisconnected(socketServerClient);
                }
            });
            return;
        }

        ArrayList<SocketServerDelegate> copyList =
                (ArrayList<SocketServerDelegate>) getSocketServerDelegates().clone();
        int count = copyList.size();
        for (int i = 0; i < count; ++i) {
            copyList.get(i).onClientDisconnected(this, socketServerClient);
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
            while (!Thread.interrupted()
                   && self.__i__checkServerSocketAvailable()) {
                Socket socket = null;
                try {
                    socket = self.getRunningServerSocket().accept();


                    SocketServerClient socketServerClient = self.internalGetSocketServerClient(socket);
                    getRunningSocketServerClients().add(socketServerClient);
                    socketServerClient.registerSocketClientDelegate(self);
                    self.__i__onSocketServerClientConnected(socketServerClient);
                }
                catch (IOException e) {
//                    e.printStackTrace();
                }
            }

            setRunning(false);

            self.setListening(false);
            self.setListenThread(null);
            self.setRunningServerSocket(null);

            self. __i__disconnectAllClients();
            self.__i__onSocketServerStopListen();
        }
    }
}