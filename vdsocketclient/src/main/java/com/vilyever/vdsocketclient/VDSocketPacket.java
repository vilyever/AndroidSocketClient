package com.vilyever.vdsocketclient;

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

    private int ID = atomicInteger.getAndIncrement();
    private byte[] data;

    
    /* #Constructors */
    public VDSocketPacket(String message) {
        self.pack(message);
    }
    
    /* #Overrides */    
    
    /* #Accessors */
    public int getID() {
        return ID;
    }

    public byte[] getPacket() {
        return data;
    }
     
    /* #Delegates */     
     
    /* #Private Methods */    
    
    /* #Public Methods */
    public void pack(String message) {
        message += "\r\n";
        try {
            self.data = message.getBytes("UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* #Classes */

    /* #Interfaces */     
     
    /* #Annotations @interface */    
    
    /* #Enums */
}