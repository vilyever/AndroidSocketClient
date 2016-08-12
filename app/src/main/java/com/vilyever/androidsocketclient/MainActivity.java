package com.vilyever.androidsocketclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    final MainActivity self = this;

    private ImageView imageView;
    protected ImageView getImageView() { if (this.imageView == null) { this.imageView = (ImageView) findViewById(R.id.imageView); } return this.imageView; }

    private TestServer testServer;
    protected TestServer getTestServer() {
        if (this.testServer == null) {
            this.testServer = new TestServer();
        }
        return this.testServer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("logger", "ts " + bytesToHex(encodeUINT16(18005)));
        Log.d("logger", "ts " + bytesToHex(encodeUINT16(18005)));

        getTestServer().beginListen();
    }

    public byte[] encodeUINT16(int param) {
        byte[] bytes = ByteBuffer.allocate(4).putInt(param ^ 0xADAD).array();

        byte[] encodedBytes = new byte[2];
        encodedBytes[0] = bytes[3];
        encodedBytes[1] = bytes[2];
        return encodedBytes;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
