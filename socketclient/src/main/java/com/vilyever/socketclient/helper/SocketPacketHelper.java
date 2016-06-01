package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.util.CharsetUtil;

/**
 * SocketPacketHelper
 * Created by vilyever on 2016/5/19.
 * Feature:
 */
public class SocketPacketHelper {
    final SocketPacketHelper self = this;

    public static final int SegmentLengthMax = -1;
    
    /* Constructors */
    public SocketPacketHelper(String charsetName) {
        this.charsetName = charsetName;
    }
    
    /* Public Methods */
    public SocketPacketHelper setSendHeaderString(String message) {
        if (message == null) {
            setSendHeaderData(null);
        }
        else {
            setSendHeaderData(CharsetUtil.stringToData(message, getCharsetName()));
        }
        return this;
    }

    public SocketPacketHelper setSendTrailerString(String message) {
        if (message == null) {
            setSendTrailerData(null);
        }
        else {
            setSendTrailerData(CharsetUtil.stringToData(message, getCharsetName()));
        }
        return this;
    }

    public SocketPacketHelper setReceiveHeaderString(String message) {
        if (message == null) {
            setReceiveHeaderData(null);
        }
        else {
            setReceiveHeaderData(CharsetUtil.stringToData(message, getCharsetName()));
        }
        return this;
    }

    public SocketPacketHelper setReceiveTrailerString(String message) {
        if (message == null) {
            setReceiveTrailerData(null);
        }
        else {
            setReceiveTrailerData(CharsetUtil.stringToData(message, getCharsetName()));
        }
        return this;
    }

    public SocketPacketHelper copy() {
        SocketPacketHelper helper = new SocketPacketHelper(getCharsetName());
        helper.setSendHeaderData(getSendHeaderData());
        helper.setSendTrailerData(getSendTrailerData());
        helper.setReceiveHeaderData(getReceiveHeaderData());
        helper.setReceiveTrailerData(getReceiveTrailerData());
        helper.setSegmentLength(getSegmentLength());

        return helper;
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
     * 发送消息时自动添加的头部
     * 用于解决分包
     */
    private byte[] sendHeaderData;
    public SocketPacketHelper setSendHeaderData(byte[] sendHeaderData) {
        this.sendHeaderData = sendHeaderData;
        return this;
    }
    public byte[] getSendHeaderData() {
//        if (getSendTrailerData() == null) {
//            return null;
//        }
        return this.sendHeaderData;
    }

    /**
     * 发送消息时自动添加的尾部信息
     * 可设为换行符，远程端即可readLine
     */
    private byte[] sendTrailerData;
    public SocketPacketHelper setSendTrailerData(byte[] sendTrailerData) {
        this.sendTrailerData = sendTrailerData;
        return this; 
    }
    public byte[] getSendTrailerData() {
        return this.sendTrailerData;
    }

    /**
     * 接收消息时每一条消息的头部信息
     * 若不为null，每一条接收消息都必须带有此头部信息，否则将无法读取
     * 回调消息时将会自动去除此信息
     */
    private byte[] receiveHeaderData;
    public SocketPacketHelper setReceiveHeaderData(byte[] receiveHeaderData) {
        this.receiveHeaderData = receiveHeaderData;
        return this;
    }
    public byte[] getReceiveHeaderData() {
//        if (getReceiveTrailerData() == null) {
//            return null;
//        }
        return this.receiveHeaderData;
    }

    /**
     * 接收消息时每一条消息的尾部信息
     * 若不为null，每一条接收消息都必须带有此尾部信息，否则将与下一次输入流合并
     * 回调消息时将会自动去除此信息
     */
    private byte[] receiveTrailerData;
    public SocketPacketHelper setReceiveTrailerData(byte[] receiveTrailerData) {
        this.receiveTrailerData = receiveTrailerData;
        return this;
    }
    public byte[] getReceiveTrailerData() {
        return this.receiveTrailerData;
    }

    /**
     * 发送消息时分段发送的每段大小
     * 分段发送可以回调进度
     * 此数值表示每次发送byte的长度
     * -1表示不分段
     */
    private int segmentLength = SegmentLengthMax;
    public SocketPacketHelper setSegmentLength(int segmentLength) {
        this.segmentLength = segmentLength;
        return this;
    }
    public int getSegmentLength() {
        if (this.segmentLength <= 0) {
            this.segmentLength = SegmentLengthMax;
        }
//        if (getSendTrailerData() == null) {
//            return SegmentLengthMax;
//        }
        return this.segmentLength;
    }

    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}