package com.vilyever.socketclient.helper;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SocketPacket
 * AndroidSocketClient <com.vilyever.vdsocketclient>
 * Created by vilyever on 2015/9/15.
 * Feature:
 */
public class SocketPacket {
    private final SocketPacket self = this;

    private static final AtomicInteger IDAtomic = new AtomicInteger();

    /* Constructors */
    public SocketPacket(byte[] data) {
        this.ID = IDAtomic.getAndIncrement();
        this.data = Arrays.copyOf(data, data.length);
        this.message = null;
    }

    public SocketPacket(String message) {
        this.ID = IDAtomic.getAndIncrement();
        this.message = message;
        this.data = null;
    }

    public static SocketPacket newInstanceWithBytes(byte[] data) {
        return newInstanceWithData(data);
    }

    public static SocketPacket newInstanceWithData(byte[] data) {
        return new SocketPacket(data);
    }

    public static SocketPacket newInstanceWithString(String message) {
        return new SocketPacket(message);
    }

    /* Public Methods */

    /* Properties */
    /**
     * ID, unique
     */
    private final int ID;
    public int getID() {
        return this.ID;
    }

    /**
     * string data
     */
    private final String message;
    public String getMessage() {
        return this.message;
    }

    /**
     * bytes data
     */
    private final byte[] data;
    public byte[] getData() {
        return this.data;
    }

    private boolean canceled;
    public SocketPacket setCanceled(boolean canceled) {
        this.canceled = canceled;
        return this;
    }
    public boolean isCanceled() {
        return this.canceled;
    }

    private float sendingProgress;
    public SocketPacket setSendingProgress(float sendingProgress) {
        this.sendingProgress = sendingProgress;
        return this;
    }
    public float getSendingProgress() {
        return this.sendingProgress;
    }
}