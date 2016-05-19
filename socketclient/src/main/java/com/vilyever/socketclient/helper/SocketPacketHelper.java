package com.vilyever.socketclient.helper;

import java.nio.charset.Charset;

/**
 * SocketPacketHelper
 * Created by vilyever on 2016/5/19.
 * Feature:
 */
public class SocketPacketHelper {
    final SocketPacketHelper self = this;
    
    
    /* Constructors */
    public SocketPacketHelper(String charsetName) {
        this.charsetName = charsetName;
    }
    
    /* Public Methods */
    public SocketPacketHelper setSendTailString(String message) {
        if (message == null) {
            setSendTailData(null);
        }
        else {
            setSendTailData(message.getBytes(Charset.forName(getCharsetName())));
        }
        return this;
    }

    public SocketPacketHelper setReceiveTailString(String message) {
        if (message == null) {
            setReceiveTailData(null);
        }
        else {
            setReceiveTailData(message.getBytes(Charset.forName(getCharsetName())));
        }
        return this;
    }
    
    /* Properties */
    private String charsetName;
    public SocketPacketHelper setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        return this;
    }
    public String getCharsetName() {
        return this.charsetName;
    }
    /**
     * 发送消息时自动添加的尾部信息
     * 可设为换行符，远程端即可readLine
     */
    private byte[] sendTailData;
    public SocketPacketHelper setSendTailData(byte[] sendTailData) {
        this.sendTailData = sendTailData;
        return this; 
    }
    public byte[] getSendTailData() {
        return this.sendTailData;
    }

    /**
     * 接收消息时每一条消息的尾部信息
     * 若不为null，每一条接收消息都必须带有此尾部信息，否则将与下一次输入流合并
     * 回调消息时将会自动去除尾部信息
     */
    private byte[] receiveTailData;
    public SocketPacketHelper setReceiveTailData(byte[] receiveTailData) {
        this.receiveTailData = receiveTailData;
        return this;
    }
    public byte[] getReceiveTailData() {
        return this.receiveTailData;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}