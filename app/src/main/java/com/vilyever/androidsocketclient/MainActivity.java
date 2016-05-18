package com.vilyever.androidsocketclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.vilyever.jsonmodel.JsonModel;
import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.SocketPacket;
import com.vilyever.socketclient.SocketResponsePacket;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.util.IPUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

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
//                        Logger.log("serverListeningSocketServerClient onResponse \n" + responsePacket.getMessage());
                        Logger.log("serverListeningSocketServerClient onResponse length " + responsePacket.getMessage().length());
                        Log.d("Logger", "serverListeningSocketServerClient onResponse \n" + responsePacket.getMessage());
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

                getLocalSocketClient().send("你也好");




                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        self.getLocalSocketClient().send("ABC");

                        TestModel testModel = new TestModel();
                        testModel.subModels = new ArrayList<TestSubModel>();
                        for (int i = 0; i < 100000; i++) {
                            TestSubModel subModel = new TestSubModel();
                            subModel.title = "title " + i;
                            testModel.subModels.add(subModel);
                        }

                        SocketPacket packet = self.getLocalSocketClient().send(testModel.toJson().toString());
                        Logger.log("packet size " + packet.getMessage().getBytes().length);

                        self.getLocalSocketClient().send("一二三");
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                    }
                }.execute();
            }
        }, 5 * 1000);


    }

    class TestModel extends JsonModel {
        ArrayList<TestSubModel> subModels;
    }

    class TestSubModel extends JsonModel {
        String title;
    }
}
