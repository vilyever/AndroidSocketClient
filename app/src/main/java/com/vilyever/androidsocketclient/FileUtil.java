package com.vilyever.androidsocketclient;

import com.vilyever.contextholder.ContextHolder;

import java.io.File;

/**
 * FileUtil
 * Created by vilyever on 2016/6/1.
 * Feature:
 */
public class FileUtil {
    final FileUtil self = this;
    
    
    /* Constructors */
    
    
    /* Public Methods */
    public static File getCacheDir() {
        File dir = null;
        dir = ContextHolder.getContext().getExternalCacheDir();
        if (dir != null && dir.isDirectory()) {
            return dir;
        }

        dir = ContextHolder.getContext().getCacheDir();
        if (dir != null && dir.isDirectory()) {
            return dir;
        }

        return dir;
    }
    
    /* Properties */
    
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}