package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.util.StringValidation;

import java.net.InetSocketAddress;

/**
 * SocketClientAddress
 * Created by vilyever on 2016/5/31.
 * Feature:
 */
public class SocketClientAddress {
    final SocketClientAddress self = this;

    public static final int DefaultConnectionTimeout = 1000 * 15;
    
    /* Constructors */
    public SocketClientAddress() {
        this(null, null);
    }


    public SocketClientAddress(String remoteIP, int remotePort) {
        this(remoteIP, "" + remotePort);
    }

    public SocketClientAddress(String remoteIP, int remotePort, int connectionTimeout) {
        this(remoteIP, "" + remotePort, connectionTimeout);
    }

    public SocketClientAddress(String remoteIP, String remotePort) {
        this(remoteIP, remotePort, DefaultConnectionTimeout);
    }

    public SocketClientAddress(String remoteIP, String remotePort, int connectionTimeout) {
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.connectionTimeout = connectionTimeout;
    }

    public SocketClientAddress copy() {
        SocketClientAddress address = new SocketClientAddress(getRemoteIP(), getRemotePort(), getConnectionTimeout());
        address.setOriginal(this);
        return address;
    }

    /* Public Methods */
    public void checkValidation() {
        if (!StringValidation.validateRegex(getRemoteIP(), StringValidation.RegexIP)) {
            throw new IllegalArgumentException("we need a correct remote IP to connect. Current is " + getRemoteIP());
        }

        if (!StringValidation.validateRegex(getRemotePort(), StringValidation.RegexPort)) {
            throw new IllegalArgumentException("we need a correct remote port to connect. Current is " + getRemotePort());
        }

        if (getConnectionTimeout() < 0) {
            throw new IllegalArgumentException("we need connectionTimeout > 0. Current is " + getConnectionTimeout());
        }
    }

    public SocketClientAddress setRemotePortWithInteger(int port) {
        setRemotePort("" + port);
        return this;
    }

    public int getRemotePortIntegerValue() {
        if (getRemotePort() == null) {
            return 0;
        }

        return Integer.valueOf(getRemotePort());
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getRemoteIP(), getRemotePortIntegerValue());
    }
    
    /* Properties */
    private SocketClientAddress original;
    protected SocketClientAddress setOriginal(SocketClientAddress original) {
        this.original = original;
        return this;
    }
    public SocketClientAddress getOriginal() {
        if (this.original == null) {
            return this;
        }
        return this.original;
    }

    /**
     * 远程IP
     */
    private String remoteIP;
    public SocketClientAddress setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
        return this;
    }
    public String getRemoteIP() {
        return this.remoteIP;
    }

    /**
     * 远程端口
     */
    private String remotePort;
    public SocketClientAddress setRemotePort(String remotePort) {
        this.remotePort = remotePort;
        return this;
    }
    public String getRemotePort() {
        return this.remotePort;
    }

    /**
     * 连接超时时间
     */
    private int connectionTimeout;
    public SocketClientAddress setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }
    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    
    /* Overrides */


    /* Delegates */
    
    
    /* Private Methods */
    
}