package com.vilyever.socketclient.util;

import java.util.Arrays;

/**
 * BytesWrapper
 * AndroidSocketClient <com.vilyever.socketclient.util>
 * Created by vilyever on 2016/4/27.
 * Feature:
 */
public class BytesWrapper {
    final BytesWrapper self = this;
    
    
    /* Constructors */
    public BytesWrapper(byte[] bytes) {
        if (bytes == null)  {
            throw new NullPointerException();
        }
        this.bytes = bytes;
    }
    
    /* Public Methods */
    public boolean equalsBytes(byte[] bytes) {
        return Arrays.equals(getBytes(), bytes);
    }
    
    /* Properties */
    private final byte[] bytes;
    public byte[] getBytes() {
        return this.bytes;
    }
    
    /* Overrides */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof BytesWrapper)) {
            return false;
        }
        return equalsBytes(((BytesWrapper)other).getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getBytes());
    }
    
    /* Delegates */
    
    
    /* Private Methods */
    
}