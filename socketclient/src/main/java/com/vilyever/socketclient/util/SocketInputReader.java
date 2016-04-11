package com.vilyever.socketclient.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * SocketInputReader
 * AndroidSocketClient <com.vilyever.socketclient.util>
 * Created by vilyever on 2016/4/11.
 * Feature:
 */
public class SocketInputReader extends Reader {
    final SocketInputReader self = this;

    private InputStream inputStream;


    /* Constructors */
    public SocketInputReader(InputStream inputStream) {
        super(inputStream);
        this.inputStream = inputStream;
    }
    
    /* Public Methods */
    
    
    /* Properties */
    
    
    /* Overrides */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }
        }
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        throw new IOException("read() is not support for SocketInputReader, try readBytes().");
    }

    public byte[] readBytes() throws IOException {
        synchronized (lock) {
            if (!isOpen()) {
                throw new IOException("InputStreamReader is closed");
            }

            try {
                ArrayList<Byte> list = new ArrayList<>();
                int c;
                while (-1 != (c = this.inputStream.read())) {
                    list.add((byte) c);

                    if (list.size() > 2
                        && list.get(list.size() - 1) == SocketSplitter.SplitterLast
                        && list.get(list.size() - 2) == SocketSplitter.SplitterFirst) {
                        list.remove(list.size() - 1);
                        list.remove(list.size() - 1);
                        break;
                    }

                    if (this.inputStream.available() == 0) {
                        break;
                    }
                }

                if (list.size() == 0) {
                    return null;
                }

                byte[] result = new byte[list.size()];
                Iterator<Byte> iterator = list.iterator();
                for (int i = 0; i < result.length; i++) {
                    result[i] = iterator.next();
                }

                return result;
            }
            catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            if (this.inputStream == null) {
                throw new IOException("InputStreamReader is closed");
            }
            try {
                return this.inputStream.available() > 0;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /* Delegates */
     
     
    /* Private Methods */
    public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException("arrayLength=" + arrayLength + "; offset=" + offset
                                                     + "; count=" + count);
        }
    }

    private boolean isOpen() {
        return this.inputStream != null;
    }
}