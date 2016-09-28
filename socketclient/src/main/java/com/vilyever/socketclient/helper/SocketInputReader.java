package com.vilyever.socketclient.helper;

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

    public byte[] readToLength(int length) throws IOException {
        if (length <= 0) {
            return null;
        }

        synchronized (lock) {
            if (!__i__isOpen()) {
                throw new IOException("InputStreamReader is closed");
            }

            try {
                byte[] buffer = new byte[length];
                int index = 0;
                int readCount = 0;

                do {
                    readCount = this.inputStream.read(buffer, index, length - index);
                    index += readCount;
                } while (readCount != -1 && index < length);

                if (index != length) {
                    return null;
                }

                return buffer;
            }
            catch (IOException e) {
                return null;
            }
        }
    }

    public byte[] readToData(byte[] data, boolean includeData) throws IOException {
        if (data == null
                || data.length <= 0) {
            return null;
        }

        synchronized (lock) {
            if (!__i__isOpen()) {
                throw new IOException("InputStreamReader is closed");
            }

            try {
                ArrayList<Byte> list = new ArrayList<>();
                int c;

                int matchIndex = 0;

                while (-1 != (c = this.inputStream.read())) {
                    list.add((byte) c);
                    if (c == (0xff & data[matchIndex])) {
                        matchIndex++;
                    }
                    else {
                        matchIndex = 0;
                    }

                    if (matchIndex == data.length) {
                        break;
                    }
                }

                if (list.size() == 0) {
                    return null;
                }

                int resultLength = list.size() - (includeData ? 0 : data.length);
                byte[] result = new byte[resultLength];
                Iterator<Byte> iterator = list.iterator();
                for (int i = 0; i < resultLength; i++) {
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
    public static void __i__checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException("arrayLength=" + arrayLength + "; offset=" + offset
                                                     + "; count=" + count);
        }
    }

    private boolean __i__isOpen() {
        return this.inputStream != null;
    }
}