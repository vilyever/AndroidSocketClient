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

    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    Thread connectThread;
    Thread sendThread;
    Thread receiveThread;
    UIHandler uiHandler = new UIHandler();

    LinkedBlockingQueue<VDSocketPacket> requestQueue = new LinkedBlockingQueue<>();
    private final Object sendLock = new Object();

    protected String ip = "127.0.0.1";
    protected int port = 80;
    protected int connectingTimeout = 15 * 1000;
    protected Map<String, String> heartbeatDictionary = new HashMap<>();

    protected List<VDSocketClientDelegate> delegates = new ArrayList<>();

    protected State state = State.Disconnected;
    
    /* #Constructors */    
    
    /* #Overrides */    
    
    /* #Accessors */
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectingTimeout() {
        return connectingTimeout;
    }

    public void setConnectingTimeout(int connectingTimeout) {
        this.connectingTimeout = connectingTimeout;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /* #Delegates */
     
    /* #Private Methods */

    /* #Protected Methods */
    protected void onConnected() {
        for (VDSocketClientDelegate delegate : self.delegates) {
            if (delegate != null) {
                delegate.didConnectFromSocketClient(self);
            }
        }
    }

    protected void onDisconnected() {
        for (VDSocketClientDelegate delegate : self.delegates) {
            if (delegate != null) {
                delegate.didDisconnectFromSocketClient(self);
            }
        }
    }

    // 异步执行，可集成此类重写回调，实现接收数据时统一预处理
    protected Object willParseResponse(String response) {
        return null;
    }

    protected void onResponse(String response) {
        for (VDSocketClientDelegate delegate : self.delegates) {
            if (delegate != null) {
                delegate.didReceiveResponseFromSocketClient(self, response);
            }
        }
    }

    /* #Public Methods */
    public synchronized boolean connect() {
        return connect(self.getIp(), self.getPort());
    }

    public synchronized boolean connect(String ip, int port) {
        return connect(ip, port, self.getConnectingTimeout());
    }

    public synchronized boolean connect(String ip, int port, int timeout) {
        self.setIp(ip);
        self.setPort(port);
        self.setConnectingTimeout(timeout);

        if (self.getState() == State.Connected
                || self.getState() == State.Connecting) {
            return false;
        }

        self.connectThread = new Thread(new ConnectRunnbale());
        self.connectThread.start();

        return true;
    }

    public synchronized void disconnect() {
        if (self.getState() == State.Disconnected) {
            return;
        }

        try {
            if (self.socket != null) {
                self.socket.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.socket = null;
        }

        try {
            if (self.outputStream != null) {
                self.outputStream.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.outputStream = null;
        }

        try {
            if (self.inputStream != null) {
                self.inputStream.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.inputStream = null;
        }

        try {
            if (self.connectThread != null && self.connectThread.isAlive()) {
                self.connectThread.interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.connectThread = null;
        }

        try {
            if (self.sendThread != null && self.sendThread.isAlive()) {
                self.sendThread.interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.sendThread = null;
        }

        try {
            if (self.receiveThread != null && self.receiveThread.isAlive()) {
                self.receiveThread.interrupt();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            self.receiveThread = null;
        }

        self.requestQueue.clear();

        Message message = new Message();
        message.what = UIHandlerEvent.Disconnected.ordinal();
        self.uiHandler.sendMessage(message);
    }

    public int send(VDSocketPacket packet) {
        self.requestQueue.add(packet);
        synchronized (self.sendLock) {
            self.sendLock.notifyAll();
        }
        return packet.getID();
    }

    public void cancel(int packetID) {
        Iterator<VDSocketPacket> iterator = self.requestQueue.iterator();
        while (iterator.hasNext()) {
            VDSocketPacket packet = iterator.next();
            if (packet.getID() == packetID) {
                iterator.remove();
            }
        }
    }

    public void addHeartbeatPair(String recieve, String send) {
        self.heartbeatDictionary.put(recieve, send);
    }

    public void registerDelegate(VDSocketClientDelegate delegate) {
        self.delegates.add(delegate);
    }

    public void unregisterDelegate(VDSocketClientDelegate delegate) {
        self.delegates.remove(delegate);
    }

    /* #Classes */
    private class ConnectRunnbale implements Runnable {
        public void run() {
            if (self.getState() == State.Disconnected) {
                try {
                    self.setState(State.Connecting);
                    self.socket = new Socket();
                    self.socket.connect(new InetSocketAddress(self.getIp(), self.getPort()), self.getConnectingTimeout());

                    self.socket.setTcpNoDelay(true);

                    Message message = new Message();
                    message.what = UIHandlerEvent.Connected.ordinal();
                    self.uiHandler.sendMessage(message);
                }
                catch (Exception e) {
                    e.printStackTrace();

                    self.disconnect();
                }

                if (self.socket != null && self.socket.isConnected()) {
                    try {
                        self.outputStream = self.socket.getOutputStream();
                        self.inputStream = self.socket.getInputStream();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    self.sendThread = new Thread(new SendRunnable());
                    self.receiveThread = new Thread(new ReceiveRunnable());
                    self.sendThread.start();
                    self.receiveThread.start();
                }
            }
        }
    }

    private class SendRunnable implements Runnable {
        public void run() {
            try {
                while (self.getState() == State.Connected
                        && self.outputStream != null) {
                    VDSocketPacket packet;
                    while ((packet = self.requestQueue.poll()) != null) {
                        self.outputStream.write(packet.getPacket());
                        self.outputStream.flush();
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
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(self.inputStream, "UTF-8"));
                while (self.getState() == State.Connected) {
                    String response = bufferedReader.readLine();

                    if (response == null) {
                        self.disconnect();
                        break;
                    }

                    if (self.heartbeatDictionary.containsKey(response)) {
                        // 自动回应心跳包
                        self.send(new VDSocketPacket(self.heartbeatDictionary.get(response)));
                    }
                    else {
                        self.willParseResponse(response);

                        Message message = new Message();
                        message.what = UIHandlerEvent.Response.ordinal();
                        message.obj = response;
                        self.uiHandler.sendMessage(message);
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

    /* #Interfaces */
    public interface VDSocketClientDelegate {
        void didConnectFromSocketClient(VDSocketClient client);
        void didDisconnectFromSocketClient(VDSocketClient client);
        void didReceiveResponseFromSocketClient(VDSocketClient client, String response);
    }

    /* #Annotations @interface */    
    
    /* #Enums */
    public enum State {
        Disconnected, Connecting, Connected, Disconnecting
    }

    private enum UIHandlerEvent {
        Connected,
        Disconnected,
        Response
    }
}