package com.vilyever.socketclient;

import com.vilyever.socketclient.util.CharsetNames;
import com.vilyever.socketclient.util.SocketSplitter;

import java.nio.charset.Charset;
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

    /**
     * sending this message every heartbeat to make sure current client alive
     */
    public static final byte[] DefaultHeartBeatMessage = "$HB$".getBytes(Charset.forName(CharsetNames.UTF_8));

    /**
     * sending DefaultPollingQueryMessage will response DefaultPollingResponseMessage immediately
     * sending DefaultPollingResponseMessage will response nothing
     */
    public static final byte[] DefaultPollingQueryMessage = "$PQ$".getBytes(Charset.forName(CharsetNames.UTF_8));
    public static final byte[] DefaultPollingResponseMessage = "$PR$".getBytes(Charset.forName(CharsetNames.UTF_8));

    private static final AtomicInteger IDAtomic = new AtomicInteger();

    /* Constructors */
    public SocketPacket(byte[] data) {
        this(data, false);
    }

    public SocketPacket(byte[] data, boolean isSupportReadLine) {
        this.ID = IDAtomic.getAndIncrement();

        if (isSupportReadLine) {
            this.data = Arrays.copyOf(data, data.length + 2);
            this.data[this.data.length - 2] = SocketSplitter.SplitterFirst;
            this.data[this.data.length - 1] = SocketSplitter.SplitterLast;
        }
        else {
            this.data = Arrays.copyOf(data, data.length);
        }

        this.message = null;
    }

    public SocketPacket(String message) {
        this(message, false);
    }

    public SocketPacket(String message, boolean isSupportReadLine) {
        this(message, CharsetNames.UTF_8, isSupportReadLine);
    }

    public SocketPacket(String message, String charsetName) {
        this(message, charsetName, false);
    }

    public SocketPacket(String message, String charsetName, boolean isSupportReadLine) {
        this(message, Charset.forName(charsetName), isSupportReadLine);
    }

    public SocketPacket(String message, Charset charset) {
        this(message, charset, false);
    }

    public SocketPacket(String message, Charset charset, boolean isSupportReadLine) {
        this.ID = IDAtomic.getAndIncrement();
        this.message = message;

        if (isSupportReadLine) {
            message += SocketSplitter.Splitter;
        }
        byte[] data = message.getBytes(charset);

        this.data = data;
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

}