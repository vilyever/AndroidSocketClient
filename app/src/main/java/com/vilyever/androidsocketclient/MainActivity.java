package com.vilyever.androidsocketclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.vilyever.socketclient.VDSocketClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VDSocketClient socketClient = new VDSocketClient();
        socketClient.registerDelegate(new VDSocketClient.VDSocketClientDelegate() {
            @Override
            public void didConnect(VDSocketClient client) {

            }

            @Override
            public void didDisconnect(VDSocketClient client) {

            }

            @Override
            public void didReceiveResponse(VDSocketClient client, String response) {

            }
        });
        socketClient.connect("192.168.1.1", 80);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
