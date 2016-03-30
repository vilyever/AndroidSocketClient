package com.vilyever.androidsocketclient;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.util.IPUtil;

public class MainActivity extends AppCompatActivity {
    final MainActivity self = this;

    private SocketServer socketServer;
    protected SocketServer getSocketServer() {
        if (this.socketServer == null) {
            this.socketServer = new SocketServer(2333);
        }
        return this.socketServer;
    }

    private SocketClient localSocketClient;
    protected SocketClient getLocalSocketClient() {
        if (this.localSocketClient == null) {
            this.localSocketClient = new SocketClient("192.168.1.153", 2333);
        }
        return this.localSocketClient;
    }

    private SocketServerClient serverListeningSocketServerClient;
    protected MainActivity setServerListeningSocketServerClient(SocketServerClient serverListeningSocketServerClient) {
        this.serverListeningSocketServerClient = serverListeningSocketServerClient;
        return this;
    }
    protected SocketClient getServerListeningSocketServerClient() {
        return this.serverListeningSocketServerClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSocketServer().registerSocketServerDelegate(new SocketServer.SocketServerDelegate() {
            @Override
            public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                System.out.println("socketServer onClientConnected");

                self.setServerListeningSocketServerClient(socketServerClient);
                socketServerClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
                    @Override
                    public void onConnected(SocketClient client) {

                    }

                    @Override
                    public void onDisconnected(SocketClient client) {
                        System.out.println("serverListeningSocketServerClient onDisconnected");
                    }

                    @Override
                    public void onResponse(SocketClient client, @NonNull String response) {
                        System.out.println("serverListeningSocketServerClient onResponse \n" + response);
                    }
                });
            }

            @Override
            public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                System.out.println("socketServer onClientDisconnected");
                self.setServerListeningSocketServerClient(null);
            }
        });
        getSocketServer().beginListen();
        getSocketServer().setHeartBeatInterval(1000 * 120);

        getLocalSocketClient().registerSocketDelegate(new SocketClient.SocketDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                System.out.println("localSocketClient onConnected");
            }

            @Override
            public void onDisconnected(SocketClient client) {
                System.out.println("localSocketClient onDisconnected");
            }

            @Override
            public void onResponse(SocketClient client, @NonNull String response) {
                System.out.println("localSocketClient onResponse \n" + response);
            }
        });

        getLocalSocketClient().connect();

        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocalSocketClient().send("haha test");
            }
        }, 15 * 1000);
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
//                getLocalSocketClient().send(SocketPacket.DefaultPollingQueryMessage);
                getLocalSocketClient().disconnect();
            }
        }, 30 * 1000);


        Logger.log("ip " + IPUtil.getIPAddress(true));
    }

}
