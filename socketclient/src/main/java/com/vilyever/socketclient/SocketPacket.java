package com.vilyever.socketclient;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SocketPacket
 * AndroidSocketClient <com.vilyever.vdsocketclient>
 * Created by vilyever on 2015/9/15.
 * Feature:
 */
public class SocketPacket {
    private final SocketPacket self = this;

    /**
     * sending this message every heartbeat to make sure current client alive
     */
    public static final String DefaultHeartBeatMessage = "$HB$";

    /**
     * sending DefaultPollingQueryMessage will response DefaultPollingResponseMessage immediately
     * sending DefaultPollingResponseMessage will response nothing
     */
    public static final String DefaultPollingQueryMessage = "$PQ$";
    public static final String DefaultPollingResponseMessage = "$PR$";

    private static final AtomicInteger IDAtomic = new AtomicInteger();

    /* Constructors */
    public SocketPacket(String message) {
        this.ID = IDAtomic.getAndIncrement();

        byte[] data;
        try {
            message += "\r\n";
            data = message.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            data = getHeartBeatMessageBytes();
        }

        this.data = data;
    }

    /* Public Methods */

    /**
     * default heartbeat message bytes, for encoding UTF-8 exception
     * @return
     */
    public static byte[] getHeartBeatMessageBytes() {
        return (DefaultHeartBeatMessage + "\r\n").getBytes();
    }

    /* Properties */
    /**
     * ID, unique
     */
    private final int ID;
    public int getID() {
        return this.ID;
    }

    /**
     * bytes data
     */
    private final byte[] data;
    public byte[] getPacket() {
        return this.data;
    }

}