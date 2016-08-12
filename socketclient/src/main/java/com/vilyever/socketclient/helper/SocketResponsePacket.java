package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.util.CharsetUtil;

import java.util.Arrays;

/**
 * SocketResponsePacket
 * AndroidSocketClient <com.vilyever.socketclient>
 * Created by vilyever on 2016/4/11.
 * Feature:
 */
public class SocketResponsePacket {
    final SocketResponsePacket self = this;

    
    /* Constructors */
    public SocketResponsePacket() {
    }

    
    /* Public Methods */
    public boolean isDataEqual(byte[] data) {
        return Arrays.equals(getData(), data);
    }

    public void buildStringWithCharsetName(String charsetName) {
        if (getData() != null) {
            setMessage(CharsetUtil.dataToString(getData(), charsetName));
        }
    }

    /* Properties */
    private byte[] data;
    public SocketResponsePacket setData(byte[] data) {
        this.data = data;
        return this;
    }
    public byte[] getData() {
        return this.data;
    }

    private String message;
    public SocketResponsePacket setMessage(String message) {
        this.message = message;
        return this;
    }
    public String getMessage() {
        return this.message;
    }

    private byte[] headerData;
    public SocketResponsePacket setHeaderData(byte[] headerData) {
        this.headerData = headerData;
        return this;
    }
    public byte[] getHeaderData() {
        return this.headerData;
    }

    private byte[] packetLengthData;
    public SocketResponsePacket setPacketLengthData(byte[] packetLengthData) {
        this.packetLengthData = packetLengthData;
        return this;
    }
    public byte[] getPacketLengthData() {
        return this.packetLengthData;
    }

    private byte[] trailerData;
    public SocketResponsePacket setTrailerData(byte[] trailerData) {
        this.trailerData = trailerData;
        return this;
    }
    public byte[] getTrailerData() {
        return this.trailerData;
    }

    private boolean heartBeat;
    public SocketResponsePacket setHeartBeat(boolean heartBeat) {
        this.heartBeat = heartBeat;
        return this;
    }
    public boolean isHeartBeat() {
        return this.heartBeat;
    }


    /* Overrides */
     
     
    /* Delegates */
     
     
    /* Private Methods */
    
}