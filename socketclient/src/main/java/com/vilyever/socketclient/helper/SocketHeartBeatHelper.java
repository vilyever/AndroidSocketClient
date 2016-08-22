package com.vilyever.socketclient.helper;

import java.util.Arrays;

/**
 * SocketHeartBeatHelper
 * Created by vilyever on 2016/5/19.
 * Feature:
 */
public class SocketHeartBeatHelper {
    final SocketHeartBeatHelper self = this;

    /* Constructors */
    public SocketHeartBeatHelper() {
    }

    public SocketHeartBeatHelper copy() {
        SocketHeartBeatHelper helper = new SocketHeartBeatHelper();
        helper.setOriginal(this);

        helper.setDefaultSendData(getDefaultSendData());
        helper.setSendDataBuilder(getSendDataBuilder());

        helper.setDefaultReceiveData(getDefaultReceiveData());
        helper.setReceiveHeartBeatPacketChecker(getReceiveHeartBeatPacketChecker());

        helper.setHeartBeatInterval(getHeartBeatInterval());
        helper.setSendHeartBeatEnabled(isSendHeartBeatEnabled());

        return helper;
    }

    /* Public Methods */
    public byte[] getSendData() {
        if (getSendDataBuilder() != null) {
            return getSendDataBuilder().obtainSendHeartBeatData(getOriginal());
        }

        return getDefaultSendData();
    }

    public boolean isReceiveHeartBeatPacket(SocketResponsePacket packet) {
        if (getReceiveHeartBeatPacketChecker() != null) {
            return getReceiveHeartBeatPacketChecker().isReceiveHeartBeatPacket(getOriginal(), packet);
        }

        if (getDefaultReceiveData() != null) {
            return packet.isDataEqual(getDefaultReceiveData());
        }

        return false;
    }


    /* Properties */
    private SocketHeartBeatHelper original;
    protected SocketHeartBeatHelper setOriginal(SocketHeartBeatHelper original) {
        this.original = original;
        return this;
    }
    public SocketHeartBeatHelper getOriginal() {
        if (this.original == null) {
            return this;
        }
        return this.original;
    }

    /**
     * 发送心跳包的数据
     */
    private byte[] defaultSendData;
    public SocketHeartBeatHelper setDefaultSendData(byte[] defaultSendData) {
        if (defaultSendData != null) {
            this.defaultSendData = Arrays.copyOf(defaultSendData, defaultSendData.length);
        }
        else {
            this.defaultSendData = null;
        }
        return this;
    }
    public byte[] getDefaultSendData() {
        return this.defaultSendData;
    }
    
    private SendDataBuilder sendDataBuilder;
    public SocketHeartBeatHelper setSendDataBuilder(SendDataBuilder sendDataBuilder) {
        this.sendDataBuilder = sendDataBuilder;
        return this;
    }
    public SendDataBuilder getSendDataBuilder() {
        return this.sendDataBuilder;
    }
    public interface SendDataBuilder {
        byte[] obtainSendHeartBeatData(SocketHeartBeatHelper helper);
    }

    /**
     * 接收心跳包的数据，用于过滤远程心跳包
     */
    private byte[] defaultReceiveData;
    public SocketHeartBeatHelper setDefaultReceiveData(byte[] defaultReceiveData) {
        if (defaultReceiveData != null) {
            this.defaultReceiveData = Arrays.copyOf(defaultReceiveData, defaultReceiveData.length);
        }
        else {
            this.defaultReceiveData = null;
        }
        return this;
    }
    public byte[] getDefaultReceiveData() {
        return this.defaultReceiveData;
    }

    private ReceiveHeartBeatPacketChecker receiveHeartBeatPacketChecker;
    public SocketHeartBeatHelper setReceiveHeartBeatPacketChecker(ReceiveHeartBeatPacketChecker receiveHeartBeatPacketChecker) {
        this.receiveHeartBeatPacketChecker = receiveHeartBeatPacketChecker;
        return this;
    }
    public ReceiveHeartBeatPacketChecker getReceiveHeartBeatPacketChecker() {
        return this.receiveHeartBeatPacketChecker;
    }
    public interface ReceiveHeartBeatPacketChecker {
        boolean isReceiveHeartBeatPacket(SocketHeartBeatHelper helper, SocketResponsePacket packet);
    }
    
    /**
     * 心跳包发送间隔
     */
    private long heartBeatInterval;
    public SocketHeartBeatHelper setHeartBeatInterval(long heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
        return this;
    }
    public long getHeartBeatInterval() {
        return this.heartBeatInterval;
    }

    /**
     * 是否发送心跳包
     * heartBeatInterval不大于0，返回false
     */
    private boolean sendHeartBeatEnabled;
    public SocketHeartBeatHelper setSendHeartBeatEnabled(boolean sendHeartBeatEnabled) {
        this.sendHeartBeatEnabled = sendHeartBeatEnabled;
        return this;
    }
    public boolean isSendHeartBeatEnabled() {
        if ((getDefaultSendData() == null
                && getSendDataBuilder() == null)
            || getHeartBeatInterval() <= 0) {
            return false;
        }
        return this.sendHeartBeatEnabled;
    }

    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
}