package com.vilyever.socketclient;

import com.vilyever.socketclient.util.CharsetNames;

import java.nio.charset.Charset;
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
    public SocketResponsePacket(byte[] data, String defaultCharsetName) {
        this.data = data;
        this.defaultCharsetName = defaultCharsetName;
    }

    
    /* Public Methods */
    public String getMessage() {
        return getMessage(getDefaultCharsetName());
    }

    public String getMessage(String charsetName) {
        return getMessage(Charset.forName(charsetName));
    }

    public String getMessage(Charset charset) {
        return new String(getData(), charset);
    }

    public boolean isMatch(String message) {
        return isMatch(message, CharsetNames.UTF_8);
    }

    public boolean isMatch(String message, String charsetName) {
        return isMatch(message, Charset.forName(charsetName));
    }

    public boolean isMatch(String message, Charset charset) {
        byte[] bytes = message.getBytes(charset);
        return Arrays.equals(getData(), bytes);
    }

    /* Properties */
    /**
     * bytes data
     */
    private final byte[] data;
    public byte[] getData() {
        return this.data;
    }

    private final String defaultCharsetName;
    public String getDefaultCharsetName() {
        return this.defaultCharsetName;
    }


    /* Overrides */
     
     
    /* Delegates */
     
     
    /* Private Methods */
    
}