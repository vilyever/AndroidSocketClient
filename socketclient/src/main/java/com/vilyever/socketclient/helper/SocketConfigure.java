package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.util.CharsetUtil;

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
        if (this.charsetName == null) {
            this.charsetName = CharsetUtil.UTF_8;
        }
        return this.charsetName;
    }

    private SocketPacketHelper socketPacketHelper;
    public SocketConfigure setSocketPacketHelper(SocketPacketHelper socketPacketHelper) {
        this.socketPacketHelper = socketPacketHelper.copy();
        return this;
    }
    public SocketPacketHelper getSocketPacketHelper() {
        return this.socketPacketHelper;
    }

    private HeartBeatHelper heartBeatHelper;
    public SocketConfigure setHeartBeatHelper(HeartBeatHelper heartBeatHelper) {
        this.heartBeatHelper = heartBeatHelper.copy();
        return this;
    }
    public HeartBeatHelper getHeartBeatHelper() {
        return this.heartBeatHelper;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}