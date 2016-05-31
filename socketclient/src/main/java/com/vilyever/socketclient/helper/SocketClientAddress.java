package com.vilyever.socketclient.helper;

import android.support.annotation.NonNull;

import com.vilyever.socketclient.util.ExceptionThrower;
import com.vilyever.socketclient.util.StringValidation;

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
        this("", -1);
    }

    public SocketClientAddress(@NonNull String remoteIP, int remotePort) {
        this(remoteIP, remotePort, DefaultConnectionTimeout);
    }

    public SocketClientAddress(@NonNull String remoteIP, int remotePort, int connectionTimeout) {
        this.remoteIP = remoteIP;
        this.remotePort = remotePort;
        this.connectionTimeout = connectionTimeout;
    }
    
    /* Public Methods */
    public void checkValidation() {
        if (!StringValidation.validateRegex(getRemoteIP(), StringValidation.RegexIP)) {
            ExceptionThrower.throwIllegalStateException("we need a correct remote IP to connect");
        }

        if (!StringValidation.validateRegex(String.format("%d", getRemotePort()), StringValidation.RegexPort)) {
            ExceptionThrower.throwIllegalStateException("we need a correct remote port to connect");
        }

        if (getConnectionTimeout() < 0) {
            throw new IllegalArgumentException("we need connectionTimeout > 0");
        }
    }
    
    /* Properties */
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
    private int remotePort;
    public SocketClientAddress setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        return this;
    }
    public int getRemotePort() {
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