package com.vilyever.socketclient;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * VDSocketClient
 * AndroidSocketClient <com.vilyever.vdsocketclient>
 * Created by vilyever on 2015/9/15.
 * Feature:
 */
public class VDSocketClient {
    private final VDSocketClient self = this;

    private final Object sendLock = new Object();

    /* Public Methods */
    /** @see #connect(String, int, int) */
    public synchronized void connect() {
        connect(self.getIp(), self.getPort());
    }

    /** @see #connect(String, int, int) */
    public synchronized void connect(String ip, int port) {
        connect(ip, port, self.getConnectingTimeout());
    }

    /**
     * 连接服务端
     * @param ip ip地址
     * @param port 端口
     * @param timeout 连接超时时间
     */
    public synchronized void connect(String ip, int port, int timeout) {
        self.setIp(ip);
        self.setPort(port);
        self.setConnectingTimeout(timeout);

        if (self.getState() == State.Connected
                || self.getState() == State.Connecting) {
            return;
        }

        self.setConnectThread(new Thread(new ConnectRunnbale()));
        self.getConnectThread().start();
    }

    /**
     * 断开连接
     */
    public synchronized void disconnect() {
        if (self.getState() == State.Disconnected) {
            return;
        }

        try {
            if (self.getSocket() != null) {
                self.getSocket().close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setSocket(null);
        }

        try {
            if (self.getOutputStream() != null) {
                self.getOutputStream().close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setOutputStream(null);
        }

        try {
            if (self.getInputStream() != null) {
                self.getInputStream().close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setInputStream(null);
        }

        try {
            if (self.getConnectThread() != null && self.getConnectThread().isAlive()) {
                self.getConnectThread().interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setConnectThread(null);
        }

        try {
            if (self.getSendThread() != null && self.getSendThread().isAlive()) {
                self.getSendThread().interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setSendThread(null);
        }

        try {
            if (self.getReceiveThread() != null && self.getReceiveThread().isAlive()) {
                self.getReceiveThread().interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.setReceiveThread(null);
        }

        self.getRequestQueue().clear();

        Message message = new Message();
        message.what = UIHandlerEvent.Disconnected.ordinal();
        self.getUiHandler().sendMessage(message);
    }

    /**
     * 发送数据包
     * @param packet 数据包
     * @return 此数据包的id
     */
    public int send(VDSocketPacket packet) {
        self.getRequestQueue().add(packet);
        synchronized (self.sendLock) {
            self.sendLock.notifyAll();
        }
        return packet.getID();
    }

    /**
     * 取消发送数据包，仅能取消仍在缓冲池的数据包
     * @param packetID 取消发送的数据包的id
     */
    public void cancel(int packetID) {
        Iterator<VDSocketPacket> iterator = self.getRequestQueue().iterator();
        while (iterator.hasNext()) {
            VDSocketPacket packet = iterator.next();
            if (packet.getID() == packetID) {
                iterator.remove();
            }
        }
    }

    /**
     * 添加心跳包接收和回应对应的消息
     * @param recieve 接收的心跳包消息
     * @param send 发送的心跳包消息
     */
    public void addHeartbeatPair(String recieve, String send) {
        self.getHeartbeatDictionary().put(recieve, send);
    }

    /**
     * 注册监听回调
     * @param delegate 回调接收者
     */
    public void registerDelegate(VDSocketClientDelegate delegate) {
        self.getDelegates().add(delegate);
    }

    /**
     * 取消注册监听回调
     * @param delegate 回调接收者
     */
    public void unregisterDelegate(VDSocketClientDelegate delegate) {
        self.getDelegates().remove(delegate);
    }

    /* Properties */
    private Socket socket;
    protected VDSocketClient setSocket(Socket socket) {
        this.socket = socket;
        return this;
    }
    protected Socket getSocket() {
        return socket;
    }

    private InputStream inputStream;
    protected VDSocketClient setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }
    protected InputStream getInputStream() {
        return inputStream;
    }

    private OutputStream outputStream;
    protected VDSocketClient setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }
    protected OutputStream getOutputStream() {
        return outputStream;
    }

    private Thread connectThread;
    protected VDSocketClient setConnectThread(Thread connectThread) {
        this.connectThread = connectThread;
        return this;
    }
    protected Thread getConnectThread() {
        return connectThread;
    }

    private Thread sendThread;
    protected VDSocketClient setSendThread(Thread sendThread) {
        this.sendThread = sendThread;
        return this;
    }
    private Thread getSendThread() {
        return sendThread;
    }

    private Thread receiveThread;
    protected VDSocketClient setReceiveThread(Thread receiveThread) {
        this.receiveThread = receiveThread;
        return this;
    }
    protected Thread getReceiveThread() {
        return receiveThread;
    }

    private UIHandler uiHandler = new UIHandler();
    protected UIHandler getUiHandler() {
        return uiHandler;
    }

    private LinkedBlockingQueue<VDSocketPacket> requestQueue;
    protected LinkedBlockingQueue<VDSocketPacket> getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = new LinkedBlockingQueue<VDSocketPacket>();
        }
        return requestQueue;
    }

    private Map<String, String> heartbeatDictionary;
    protected Map<String, String> getHeartbeatDictionary() {
        if (heartbeatDictionary == null) {
            heartbeatDictionary = new HashMap<String, String>();
        }
        return heartbeatDictionary;
    }

    private List<VDSocketClientDelegate> delegates;
    protected List<VDSocketClientDelegate> getDelegates() {
        if (delegates == null) {
            delegates = new ArrayList<>();
        }
        return delegates;
    }

    protected String ip = "127.0.0.1";
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }

    protected int port = 80;
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    protected int connectingTimeout = 15 * 1000;
    public int getConnectingTimeout() {
        return connectingTimeout;
    }
    public void setConnectingTimeout(int connectingTimeout) {
        this.connectingTimeout = connectingTimeout;
    }

    protected State state = State.Disconnected;
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }

    /* Protected Methods */
    protected void onConnected() {
        for (VDSocketClientDelegate delegate : self.delegates) {
            if (delegate != null) {
                delegate.didConnect(self);
            }
        }
    }

    protected void onDisconnected() {
        for (VDSocketClientDelegate delegate : self.getDelegates()) {
            if (delegate != null) {
                delegate.didDisconnect(self);
            }
        }
    }

    protected void onResponse(String response) {
        for (VDSocketClientDelegate delegate : self.getDelegates()) {
            if (delegate != null) {
                delegate.didReceiveResponse(self, response);
            }
        }
    }

    /* Classes */
    private class ConnectRunnbale implements Runnable {
        public void run() {
            if (self.getState() == State.Disconnected) {
                try {
                    self.setState(State.Connecting);
                    self.setSocket(new Socket());
                    self.getSocket().connect(new InetSocketAddress(self.getIp(), self.getPort()), self.getConnectingTimeout());

                    self.getSocket().setTcpNoDelay(true);

                    Message message = new Message();
                    message.what = UIHandlerEvent.Connected.ordinal();
                    self.getUiHandler().sendMessage(message);
                }
                catch (Exception e) {
                    e.printStackTrace();

                    self.disconnect();
                }

                if (self.getSocket() != null && self.getSocket().isConnected()) {
                    try {
                        self.setOutputStream(self.getSocket().getOutputStream());
                        self.setInputStream(self.getSocket().getInputStream());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    self.setSendThread(new Thread(new SendRunnable()));
                    self.getSendThread().start();
                    self.setReceiveThread(new Thread(new ReceiveRunnable()));
                    self.getReceiveThread().start();
                }
            }
        }
    }

    private class SendRunnable implements Runnable {
        public void run() {
            try {
                while (self.getState() == State.Connected
                        && self.getOutputStream() != null) {
                    VDSocketPacket packet;
                    while ((packet = self.getRequestQueue().poll()) != null) {
                        self.getOutputStream().write(packet.getPacket());
                        self.getOutputStream().flush();
                    }

                    synchronized (self.sendLock) {
                        self.sendLock.wait();
                    }
                }
            }
            catch (SocketException e) {
                e.printStackTrace();
                // 发送的时候出现异常，说明socket被关闭了(服务器关闭)java.net.SocketException:
                // sendto failed: EPIPE (Broken pipe)
                self.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveRunnable implements Runnable {
        public void run() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(self.getInputStream(), "UTF-8"));
                while (self.getState() == State.Connected) {
                    String response = bufferedReader.readLine();

                    if (response == null) {
                        self.disconnect();
                        break;
                    }

                    if (self.getHeartbeatDictionary().containsKey(response)) {
                        // 自动回应心跳包
                        self.send(new VDSocketPacket(self.getHeartbeatDictionary().get(response)));
                    }
                    else {
                        Message message = new Message();
                        message.what = UIHandlerEvent.Response.ordinal();
                        message.obj = response;
                        self.getUiHandler().sendMessage(message);
                    }
                }
                bufferedReader.close();
            }
            catch (SocketException e) {
                e.printStackTrace();
                // 客户端主动socket.disconnect()会调用这里
                // java.net.SocketException: Socket
                self.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            UIHandlerEvent event = UIHandlerEvent.values()[msg.what];
            switch (event) {
                case Connected:
                    self.setState(State.Connected);
                    break;
                case Disconnected:
                    self.setState(State.Disconnected);
                    break;
                case Response:
                    self.onResponse(msg.obj.toString());
                    break;
            }
        }
    }

    /* Interfaces */
    public interface VDSocketClientDelegate {
        void didConnect(VDSocketClient client);
        void didDisconnect(VDSocketClient client);
        void didReceiveResponse(VDSocketClient client, String response);
    }


    /* Enums */
    public enum State {
        Disconnected, Connecting, Connected
    }

    private enum UIHandlerEvent {
        Connected,
        Disconnected,
        Response
    }
}