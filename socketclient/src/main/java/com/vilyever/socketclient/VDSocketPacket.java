package com.vilyever.socketclient;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * VDSocketPacket
 * AndroidSocketClient <com.vilyever.vdsocketclient>
 * Created by vilyever on 2015/9/15.
 * Feature:
 */
public class VDSocketPacket {
    private final VDSocketPacket self = this;

    private static final AtomicInteger atomicInteger = new AtomicInteger();

    /* Constructors */
    public VDSocketPacket(String message) {
        self.pack(message);
    }

    /* Public Methods */
    /**
     * 打包string
     * @param message 字符串信息
     */
    public void pack(String message) {
        message += "\r\n";
        try {
            self.data = message.getBytes("UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Properties */
    private int ID = atomicInteger.getAndIncrement();
    public int getID() {
        return ID;
    }

    private byte[] data;
    public byte[] getPacket() {
        return data;
    }

}