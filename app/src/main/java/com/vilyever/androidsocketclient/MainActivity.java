package com.vilyever.androidsocketclient;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketPacket;
import com.vilyever.socketclient.SocketResponsePacket;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.util.IPUtil;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    final MainActivity self = this;

    private SocketServer socketServer;
    protected SocketServer getSocketServer() {
        if (this.socketServer == null) {
            this.socketServer = new SocketServer();
        }
        return this.socketServer;
    }

    private SocketClient localSocketClient;
    protected SocketClient getLocalSocketClient() {
        if (this.localSocketClient == null) {
            this.localSocketClient = new SocketClient(IPUtil.getIPAddress(true), getSocketServer().getPort());
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
            public void onServerBeginListen(SocketServer socketServer, int port) {
                Logger.log("begin listen " + port);
            }

            @Override
            public void onServerStopListen(SocketServer socketServer, int port) {
                Logger.log("stop listen " + port);
            }

            @Override
            public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                Logger.log("socketServer onClientConnected");

                self.setServerListeningSocketServerClient(socketServerClient);
                socketServerClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
                    @Override
                    public void onConnected(SocketClient client) {

                    }

                    @Override
                    public void onDisconnected(SocketClient client) {
                        Logger.log("serverListeningSocketServerClient onDisconnected");
                    }

                    @Override
                    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                        Logger.log("serverListeningSocketServerClient onResponse \n" + responsePacket.getMessage());
                    }
                });
            }

            @Override
            public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                Logger.log("socketServer onClientDisconnected");
                self.setServerListeningSocketServerClient(null);
            }
        });
        getSocketServer().beginListenFromPort(80);

        getLocalSocketClient().registerSocketDelegate(new SocketClient.SocketDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                Logger.log("localSocketClient onConnected");
                getLocalSocketClient().send("再见");
            }

            @Override
            public void onDisconnected(SocketClient client) {
                Logger.log("localSocketClient onDisconnected");
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                Logger.log("localSocketClient onResponse \n" + responsePacket.getMessage());
            }
        });

        getLocalSocketClient().registerSocketHeartBeatDelegate(new SocketClient.SocketHeartBeatDelegate() {
            @Override
            public void onHeartBeat(SocketClient socketClient) {
                Logger.log("onHeartBeat");
            }
        });

        getLocalSocketClient().registerSocketPollingDelegate(new SocketClient.SocketPollingDelegate() {
            @Override
            public void onPollingQuery(SocketClient socketClient, SocketResponsePacket pollingQueryPacket) {
                Logger.log("onPollingQuery " + pollingQueryPacket.getMessage());
            }

            @Override
            public void onPollingResponse(SocketClient socketClient, SocketResponsePacket pollingResponsePacket) {
                Logger.log("onPollingResponse " + pollingResponsePacket.getMessage());
            }
        });

        getLocalSocketClient().connect();

        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocalSocketClient().send("你好");
                try {
                    getLocalSocketClient().send("HELLO".getBytes("UTF-8"));
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                getLocalSocketClient().send(SocketPacket.DefaultPollingQueryMessage);
            }
        }, 5 * 1000);
    }

}
