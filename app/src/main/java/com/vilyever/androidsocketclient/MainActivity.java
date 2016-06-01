package com.vilyever.androidsocketclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.vilyever.jsonmodel.JsonModel;
import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceiveDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.server.SocketServerDelegate;
import com.vilyever.socketclient.util.IPUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final MainActivity self = this;

    private ImageView imageView;
    protected ImageView getImageView() { if (this.imageView == null) { this.imageView = (ImageView) findViewById(R.id.imageView); } return this.imageView; }

    private SocketServer socketServer;
    protected SocketServer getSocketServer() {
        if (this.socketServer == null) {
            this.socketServer = new SocketServer();

            this.socketServer.getHeartBeatHelper().setHeartBeatInterval(1000 * 30);
            this.socketServer.getHeartBeatHelper().setRemoteNoReplyAliveTimeout(1000 * 60);
            this.socketServer.getHeartBeatHelper().setSendString("$HB$");
            this.socketServer.getHeartBeatHelper().setReceiveString("$HB$");

//            this.socketServer.getSocketPacketHelper().setSendHeaderData(new byte[]{0x03,0x02});
//            this.socketServer.getSocketPacketHelper().setSendTrailerData(new byte[]{0x01,0x03});
//            this.socketServer.getSocketPacketHelper().setReceiveHeaderData(new byte[]{0x03,0x02});
//            this.socketServer.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x01,0x03});
            this.socketServer.getSocketPacketHelper().setSendHeaderString("_abcd_1f9jsld;af");
            this.socketServer.getSocketPacketHelper().setSendTrailerString("jrjgofnosd9[;[];3289-sjf");
            this.socketServer.getSocketPacketHelper().setReceiveHeaderString("_abcd_1f9jsld;af");
            this.socketServer.getSocketPacketHelper().setReceiveTrailerString("jrjgofnosd9[;[];3289-sjf");

            this.socketServer.registerSocketServerDelegate(new SocketServerDelegate() {
                @Override
                public void onServerBeginListen(SocketServer socketServer, int port) {
                    Logger.log("SocketServer: begin listen " + port);
                }

                @Override
                public void onServerStopListen(SocketServer socketServer, int port) {
                    Logger.log("SocketServer: stop listen " + port);
                }

                @Override
                public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                    Logger.log("SocketServer: onClientConnected");

                    self.setServerListeningSocketServerClient(socketServerClient);
                }

                @Override
                public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                    Logger.log("SocketServer: onClientDisconnected");
                    self.setServerListeningSocketServerClient(null);
                }
            });
        }
        return this.socketServer;
    }

    private SocketClient localSocketClient;
    protected SocketClient getLocalSocketClient() {
        if (this.localSocketClient == null) {
            this.localSocketClient = new SocketClient(new SocketClientAddress(IPUtil.getIPAddress(true), getSocketServer().getPort()));

            this.localSocketClient.getHeartBeatHelper().setHeartBeatInterval(1000 * 30);
            this.localSocketClient.getHeartBeatHelper().setRemoteNoReplyAliveTimeout(1000 * 60);
            this.localSocketClient.getHeartBeatHelper().setSendString("$HB$");
            this.localSocketClient.getHeartBeatHelper().setReceiveString("$HB$");

//            this.localSocketClient.getSocketPacketHelper().setSendHeaderData(new byte[]{0x03,0x02});
//            this.localSocketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{0x01,0x03});
//            this.localSocketClient.getSocketPacketHelper().setReceiveHeaderData(new byte[]{0x03,0x02});
//            this.localSocketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x01,0x03});

            this.localSocketClient.getSocketPacketHelper().setSendHeaderString("_abcd_1f9jsld;af");
            this.localSocketClient.getSocketPacketHelper().setSendTrailerString("jrjgofnosd9[;[];3289-sjf");
            this.localSocketClient.getSocketPacketHelper().setReceiveHeaderString("_abcd_1f9jsld;af");
            this.localSocketClient.getSocketPacketHelper().setReceiveTrailerString("jrjgofnosd9[;[];3289-sjf");

            this.localSocketClient.getSocketPacketHelper().setSegmentLength(4 * 1024);

            this.localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
                @Override
                public void onConnected(SocketClient client) {
                    Logger.log("Local onConnected");
                    getLocalSocketClient().sendString("sy hi!");
//                    getLocalSocketClient().sendString("sy hi! you are impossible warriors");
//                    getLocalSocketClient().sendString("sy hi! you are impossible warriors, i may really make a good decision to support u.");
//                    getLocalSocketClient().sendString("sy hi! you are impossible warriors, i may really make a good decision to support u.");
                }

                @Override
                public void onDisconnected(SocketClient client) {
                    Logger.log("Local onDisconnected");
                }

                @Override
                public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                }
            });
            this.localSocketClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {

                @Override
                public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
//                    Logger.log("Local onSendPacketBegin " + packet.getID());
                }

                @Override
                public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
//                    Logger.log("Local onSendPacketCancel " + packet.getID());
                }

                @Override
                public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
//                    Logger.log("Local onSendPacketEnd " + packet.getID());
                }

                @Override
                public void onSendPacketProgress(SocketClient client, SocketPacket packet, float progress) {
//                    Logger.log("Local onSendPacketProgress " + packet.getID() + "  progress : " + progress);
                }
            });
            this.localSocketClient.registerSocketClientReceiveDelegate(new SocketClientReceiveDelegate() {
                @Override
                public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                    Logger.log("Local onResponse 【" + responsePacket.getMessage() + "】");
                }

                @Override
                public void onHeartBeat(SocketClient socketClient) {
//                    Logger.log("Local onHeartBeat ");
                }
            });
        }
        return this.localSocketClient;
    }

    private SocketServerClient serverListeningSocketServerClient;
    protected MainActivity setServerListeningSocketServerClient(SocketServerClient serverListeningSocketServerClient) {
        this.serverListeningSocketServerClient = serverListeningSocketServerClient;
        if (serverListeningSocketServerClient == null) {
            return this;
        }

        this.serverListeningSocketServerClient.registerSocketClientDelegate(new SocketClientDelegate() {
            @Override
            public void onConnected(SocketClient client) {
//                Logger.log("Server onConnected");
            }

            @Override
            public void onDisconnected(SocketClient client) {
//                Logger.log("Server onDisconnected");
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
            }
        });
        this.serverListeningSocketServerClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {

            @Override
            public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
//                Logger.log("Server onSendPacketBegin " + packet.getID());
            }

            @Override
            public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
//                Logger.log("Server onSendPacketCancel " + packet.getID());
            }

            @Override
            public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
//                Logger.log("Server onSendPacketEnd " + packet.getID());

            }

            @Override
            public void onSendPacketProgress(SocketClient client, SocketPacket packet, float progress) {
//                Logger.log("Server onSendPacketProgress " + packet.getID() + "  progress : " + progress);
            }
        });
        this.serverListeningSocketServerClient.registerSocketClientReceiveDelegate(new SocketClientReceiveDelegate() {
            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                if (responsePacket.getMessage().length() < 100) {
                    Logger.log("Server onResponse 【" + responsePacket.getMessage() + "】");
                }
                else {
                    Logger.log("Server onResponse 【" + responsePacket.getMessage().substring(0, 100) + "】");
                    Logger.log("Server onResponse length 【" + responsePacket.getData().length + "】");
                }
            }

            @Override
            public void onHeartBeat(SocketClient socketClient) {
                Logger.log("Server onHeartBeat ");
            }
        });
        return this;
    }
    protected SocketClient getServerListeningSocketServerClient() {
        return this.serverListeningSocketServerClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSocketServer().beginListenFromPort(80);

        getLocalSocketClient().connect();

        boolean shouldSend = true;
        if (shouldSend) {
            getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getLocalSocketClient().sendString("你好");
                    try {
                        getLocalSocketClient().sendData("HELLO".getBytes("UTF-8"));
                    }
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    getLocalSocketClient().sendString("你也好");

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            self.getLocalSocketClient().sendString("ABC");

                            TestModel testModel = new TestModel();
                            testModel.subModels = new ArrayList<TestSubModel>();
                            for (int i = 0; i < 10000; i++) {
                                TestSubModel subModel = new TestSubModel();
                                subModel.title = "title " + i;
                                testModel.subModels.add(subModel);
                            }

                            SocketPacket packet = self.getLocalSocketClient().sendString(testModel.toJson().toString());
                            Logger.log("packet size " + packet.getMessage().getBytes().length);

                            self.getLocalSocketClient().sendString("一二三");

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
    }

    class TestModel extends JsonModel {
        ArrayList<TestSubModel> subModels;
    }

    class TestSubModel extends JsonModel {
        String title;
    }
}
