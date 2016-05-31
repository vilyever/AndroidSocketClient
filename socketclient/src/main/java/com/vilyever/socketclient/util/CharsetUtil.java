package com.vilyever.socketclient.util;

import java.io.UnsupportedEncodingException;

/**
 * CharsetUtil
 * AndroidSocketClient <com.vilyever.socketclient.util>
 * Created by vilyever on 2016/4/11.
 * Feature:
 */
public class CharsetUtil {
    final CharsetUtil self = this;

    public static final String UTF_8 = "UTF-8";
    
    /* Constructors */
    
    
    /* Public Methods */
    public static byte[] stringToData(String string, String charsetName) {
        if (string != null) {
            try {
                return string.getBytes(charsetName);
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String dataToString(byte[] data, String charsetName) {
        if (data != null) {
            try {
                return new String(data, charsetName);
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /* Properties */
    
    
    /* Overrides */
     
     
    /* Delegates */
     
     
    /* Private Methods */
    
}