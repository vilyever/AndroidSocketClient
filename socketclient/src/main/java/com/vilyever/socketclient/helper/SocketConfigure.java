package com.vilyever.socketclient.helper;

/**
 * SocketConfigure
 * Created by vilyever on 2016/5/31.
 * Feature:
 */
public class SocketConfigure {
    final SocketConfigure self = this;
    
    
    /* Constructors */
    
    
    /* Public Methods */
    
    
    /* Properties */
    private String charsetName;
    public SocketConfigure setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        return this;
    }
    public String getCharsetName() {
        return this.charsetName;
    }

    private SocketClientAddress address;
    public SocketConfigure setAddress(SocketClientAddress address) {
        this.address = address.copy();
        return this;
    }
    public SocketClientAddress getAddress() {
        return this.address;
    }

    private SocketPacketHelper socketPacketHelper;
    public SocketConfigure setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
        this.socketPacketHelper = socketPacketHelper.copy();
        return this;
    }
    public SocketPacketHelper getSocketPacketHelper() {
        return this.socketPacketHelper;
    }

    private SocketHeartBeatHelper heartBeatHelper;
    public SocketConfigure setHeartBeatHelper(SocketHeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper.copy();
        return this;
    }
    public SocketHeartBeatHelper getHeartBeatHelper() {
        return this.heartBeatHelper;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}