package com.vilyever.socketclient.helper;

import java.util.Arrays;

/**
 * SocketPacketHelper
 * Created by vilyever on 2016/5/19.
 * Feature:
 */
public class SocketPacketHelper {
    final SocketPacketHelper self = this;


    /* Constructors */
    public SocketPacketHelper() {
    }

    public SocketPacketHelper copy() {
        SocketPacketHelper helper = new SocketPacketHelper();
        helper.setOriginal(this);

        helper.setSendHeaderData(getSendHeaderData());
        helper.setSendPacketLengthDataConvertor(getSendPacketLengthDataConvertor());
        helper.setSendTrailerData(getSendTrailerData());
        helper.setSendSegmentLength(getSendSegmentLength());
        helper.setSendSegmentEnabled(isSendSegmentEnabled());
        helper.setSendTimeout(getSendTimeout());
        helper.setSendTimeoutEnabled(isSendTimeoutEnabled());

        helper.setReadStrategy(getReadStrategy());

        helper.setReceiveHeaderData(getReceiveHeaderData());
        helper.setReceivePacketLengthDataLength(getReceivePacketLengthDataLength());
        helper.setReceivePacketDataLengthConvertor(getReceivePacketDataLengthConvertor());
        helper.setReceiveTrailerData(getReceiveTrailerData());
        helper.setReceiveSegmentLength(getReceiveSegmentLength());
        helper.setReceiveSegmentEnabled(isReceiveSegmentEnabled());
        helper.setReceiveTimeout(getReceiveTimeout());
        helper.setReceiveTimeoutEnabled(isReceiveTimeoutEnabled());

        return helper;
    }

    /* Public Methods */
    public void checkValidation() {
        switch (getReadStrategy()) {
            case Manually:
                return;
            case AutoReadToTrailer:
                if (getReceiveTrailerData() == null
                        || getReceiveTrailerData().length <= 0) {
                    throw new IllegalArgumentException("we need ReceiveTrailerData for AutoReadToTrailer");
                }
                return;
            case AutoReadByLength:
                if (getReceivePacketLengthDataLength() <= 0
                        || getReceivePacketDataLengthConvertor() == null) {
                    throw new IllegalArgumentException("we need ReceivePacketLengthDataLength and ReceivePacketDataLengthConvertor for AutoReadByLength");
                }
                return;
        }

        throw new IllegalArgumentException("we need a correct ReadStrategy");
    }

    public byte[] getSendPacketLengthData(int packetLength) {
        if (getSendPacketLengthDataConvertor() != null) {
            return getSendPacketLengthDataConvertor().obtainSendPacketLengthDataForPacketLength(getOriginal(), packetLength);
        }

        return null;
    }

    public int getReceivePacketDataLength(byte[] packetLengthData) {
        if (getReadStrategy() == ReadStrategy.AutoReadByLength) {
            if (getReceivePacketDataLengthConvertor() != null) {
                return getReceivePacketDataLengthConvertor().obtainReceivePacketDataLength(getOriginal(), packetLengthData);
            }
        }

        return 0;
    }

    /* Properties */
    private SocketPacketHelper original;
    protected SocketPacketHelper setOriginal(SocketPacketHelper original) {
        this.original = original;
        return this;
    }
    public SocketPacketHelper getOriginal() {
        if (this.original == null) {
            return this;
        }
        return this.original;
    }

    /**
     * 发送消息时自动添加的包头
     */
    private byte[] sendHeaderData;
    public SocketPacketHelper setSendHeaderData(byte[] sendHeaderData) {
        if (sendHeaderData != null) {
            this.sendHeaderData = Arrays.copyOf(sendHeaderData, sendHeaderData.length);
        }
        else {
            this.sendHeaderData = null;
        }
        return this;
    }
    public byte[] getSendHeaderData() {
        return this.sendHeaderData;
    }
    
    private SendPacketLengthDataConvertor sendPacketLengthDataConvertor;
    public SocketPacketHelper setSendPacketLengthDataConvertor(SendPacketLengthDataConvertor sendPacketLengthDataConvertor) {
        this.sendPacketLengthDataConvertor = sendPacketLengthDataConvertor;
        return this;
    }
    public SendPacketLengthDataConvertor getSendPacketLengthDataConvertor() {
        return this.sendPacketLengthDataConvertor;
    }
    public interface SendPacketLengthDataConvertor {
        byte[] obtainSendPacketLengthDataForPacketLength(SocketPacketHelper helper, int packetLength);
    }

    /**
     * 发送消息时自动添加的包尾
     */
    private byte[] sendTrailerData;
    public SocketPacketHelper setSendTrailerData(byte[] sendTrailerData) {
        if (sendTrailerData != null) {
            this.sendTrailerData = Arrays.copyOf(sendTrailerData, sendTrailerData.length);
        }
        else {
            this.sendTrailerData = null;
        }
        return this; 
    }
    public byte[] getSendTrailerData() {
        return this.sendTrailerData;
    }

    /**
     * 发送消息时分段发送的每段大小
     * 分段发送可以回调进度
     * 此数值表示每次发送byte的长度
     * 不大于0表示不分段
     */
    private int sendSegmentLength;
    public SocketPacketHelper setSendSegmentLength(int sendSegmentLength) {
        this.sendSegmentLength = sendSegmentLength;
        return this;
    }
    public int getSendSegmentLength() {
        return this.sendSegmentLength;
    }

    /**
     * 若sendSegmentLength不大于0，返回false
     */
    private boolean sendSegmentEnabled;
    public SocketPacketHelper setSendSegmentEnabled(boolean sendSegmentEnabled) {
        this.sendSegmentEnabled = sendSegmentEnabled;
        return this;
    }
    public boolean isSendSegmentEnabled() {
        if (getSendSegmentLength() <= 0) {
            return false;
        }
        return this.sendSegmentEnabled;
    }

    /**
     * 发送超时时长，超过时长无法写出自动断开连接
     * 仅在每个发送包开始发送时计时，结束后重置计时
     */
    private long sendTimeout;
    public SocketPacketHelper setSendTimeout(long sendTimeout) {
        this.sendTimeout = sendTimeout;
        return this;
    }
    public long getSendTimeout() {
        return this.sendTimeout;
    }

    private boolean sendTimeoutEnabled;
    public SocketPacketHelper setSendTimeoutEnabled(boolean sendTimeoutEnabled) {
        this.sendTimeoutEnabled = sendTimeoutEnabled;
        return this;
    }
    public boolean isSendTimeoutEnabled() {
        return this.sendTimeoutEnabled;
    }

    private ReadStrategy readStrategy = ReadStrategy.Manually;
    public SocketPacketHelper setReadStrategy(ReadStrategy readStrategy) {
        this.readStrategy = readStrategy;
        return this;
    }
    public ReadStrategy getReadStrategy() {
        return this.readStrategy;
    }
    public enum ReadStrategy {
        /**
         * 手动读取
         * 手动调用{@link com.vilyever.socketclient.SocketClient#readDataToData(byte[])}或{@link com.vilyever.socketclient.SocketClient#readDataToLength(int)}读取
         */
        Manually,
        /**
         * 自动读取到包尾
         * 需设置包尾相关信息
         * 自动读取信息直到读取到与包尾相同的数据后，回调接收包
         */
        AutoReadToTrailer,
        /**
         * 自动按长度读取
         * 需设置长度相关信息
         * 自动读取包长度信息，转换成包长度后读取该长度字节后，回调接收包
         */
        AutoReadByLength,
    }

    /**
     * 接收消息时每一条消息的头部信息
     * 若不为null，每一条接收消息都必须带有此头部信息，否则将无法读取
     */
    private byte[] receiveHeaderData;
    public SocketPacketHelper setReceiveHeaderData(byte[] receiveHeaderData) {
        if (receiveHeaderData != null) {
            this.receiveHeaderData = Arrays.copyOf(receiveHeaderData, receiveHeaderData.length);
        }
        else {
            this.receiveHeaderData = null;
        }
        return this;
    }
    public byte[] getReceiveHeaderData() {
        return this.receiveHeaderData;
    }

    /**
     * 接收时，包长度data的固定字节数
     */
    private int receivePacketLengthDataLength;
    public SocketPacketHelper setReceivePacketLengthDataLength(int receivePacketLengthDataLength) {
        this.receivePacketLengthDataLength = receivePacketLengthDataLength;
        return this;
    }
    public int getReceivePacketLengthDataLength() {
        return this.receivePacketLengthDataLength;
    }

    private ReceivePacketDataLengthConvertor receivePacketDataLengthConvertor;
    public SocketPacketHelper setReceivePacketDataLengthConvertor(ReceivePacketDataLengthConvertor receivePacketDataLengthConvertor) {
        this.receivePacketDataLengthConvertor = receivePacketDataLengthConvertor;
        return this;
    }
    public ReceivePacketDataLengthConvertor getReceivePacketDataLengthConvertor() {
        return this.receivePacketDataLengthConvertor;
    }
    public interface ReceivePacketDataLengthConvertor {
        int obtainReceivePacketDataLength(SocketPacketHelper helper, byte[] packetLengthData);
    }

    /**
     * 接收消息时每一条消息的尾部信息
     * 若不为null，每一条接收消息都必须带有此尾部信息，否则将与下一次输入流合并
     */
    private byte[] receiveTrailerData;
    public SocketPacketHelper setReceiveTrailerData(byte[] receiveTrailerData) {
        if (receiveTrailerData != null) {
            this.receiveTrailerData = Arrays.copyOf(receiveTrailerData, receiveTrailerData.length);
        }
        else {
            this.receiveTrailerData = null;
        }
        return this;
    }
    public byte[] getReceiveTrailerData() {
        return this.receiveTrailerData;
    }

    /**
     * 分段接收消息，每段长度，仅在按长度读取时有效
     * 若设置大于0时，receiveSegmentEnabled自动变更为true，反之亦然
     * 设置后可手动变更receiveSegmentEnabled
     */
    private int receiveSegmentLength;
    public SocketPacketHelper setReceiveSegmentLength(int receiveSegmentLength) {
        this.receiveSegmentLength = receiveSegmentLength;
        return this;
    }
    public int getReceiveSegmentLength() {
        return this.receiveSegmentLength;
    }

    /**
     * 若receiveSegmentLength不大于0，返回false
     */
    private boolean receiveSegmentEnabled;
    public SocketPacketHelper setReceiveSegmentEnabled(boolean receiveSegmentEnabled) {
        this.receiveSegmentEnabled = receiveSegmentEnabled;
        return this;
    }
    public boolean isReceiveSegmentEnabled() {
        if (getReceiveSegmentLength() <= 0) {
            return false;
        }
        return this.receiveSegmentEnabled;
    }

    /**
     * 读取超时时长，超过时长没有读取到任何消息自动断开连接
     */
    private long receiveTimeout;
    public SocketPacketHelper setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
        return this;
    }
    public long getReceiveTimeout() {
        return this.receiveTimeout;
    }

    private boolean receiveTimeoutEnabled;
    public SocketPacketHelper setReceiveTimeoutEnabled(boolean receiveTimeoutEnabled) {
        this.receiveTimeoutEnabled = receiveTimeoutEnabled;
        return this;
    }
    public boolean isReceiveTimeoutEnabled() {
        return this.receiveTimeoutEnabled;
    }

    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}