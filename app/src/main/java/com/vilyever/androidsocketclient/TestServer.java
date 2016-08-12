package com.vilyever.androidsocketclient;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.vilyever.contextholder.ContextHolder;
import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.server.SocketServer;
import com.vilyever.socketclient.server.SocketServerClient;
import com.vilyever.socketclient.server.SocketServerDelegate;
import com.vilyever.socketclient.util.CharsetUtil;

import java.util.Arrays;

/**
 * TestServer
 * Created by vilyever on 2016/7/26.
 * Feature:
 */
public class TestServer {
    final TestServer self = this;
    
    
    /* Constructors */
    public TestServer() {

    }
    
    /* Public Methods */
    public void beginListen() {
        int port = getSocketServer().beginListenFromPort(21998);
        Toast.makeText(ContextHolder.getContext(), "port " + port, Toast.LENGTH_LONG).show();
    }
    
    /* Properties */
    private SocketServer socketServer;
    protected SocketServer getSocketServer() {
        if (this.socketServer == null) {
            this.socketServer = new SocketServer();
            this.socketServer.setCharsetName(CharsetUtil.UTF_8);

            this.socketServer.getSocketPacketHelper().setSendTrailerData(new byte[]{0x13, 0x10});

            this.socketServer.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadByLength);
            this.socketServer.getSocketPacketHelper().setReceiveHeaderData(CharsetUtil.stringToData("Local:", CharsetUtil.UTF_8));
            this.socketServer.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x13, 0x10});
            this.socketServer.getSocketPacketHelper().setReceivePacketLengthDataLength(4);
            this.socketServer.getSocketPacketHelper().setReceivePacketDataLengthConvertor(new SocketPacketHelper.ReceivePacketDataLengthConvertor() {
                @Override
                public int obtainReceivePacketDataLength(SocketPacketHelper helper, byte[] packetLengthData) {
                    int length =  (packetLengthData[3] & 0xFF) + ((packetLengthData[2] & 0xFF) << 8) + ((packetLengthData[1] & 0xFF) << 16) + ((packetLengthData[0] & 0xFF) << 24);

                    Log.d("logger", "dlen " + Arrays.toString(packetLengthData) + "  , lengt "  + length);

                    return length;
                }
            });
            this.socketServer.getSocketPacketHelper().setReceiveSegmentLength(1);
            this.socketServer.getSocketPacketHelper().setReceiveSegmentEnabled(true);

            this.socketServer.registerSocketServerDelegate(new SocketServerDelegate() {
                @Override
                public void onServerBeginListen(SocketServer socketServer, int port) {
                    Logger.log("SocketServer: begin listen " + port);
                    getTestClient().connect();
                }

                @Override
                public void onServerStopListen(SocketServer socketServer, int port) {
                    Logger.log("SocketServer: stop listen " + port);
                }

                @Override
                public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {
                    Logger.log("SocketServer: onClientConnected");

                    self.setServerListeningSocketServerClient(socketServerClient);
                    socketServerClient.sendString("from android server: hello");
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

    private SocketServerClient serverListeningSocketServerClient;
    protected TestServer setServerListeningSocketServerClient(SocketServerClient serverListeningSocketServerClient) {
        this.serverListeningSocketServerClient = serverListeningSocketServerClient;
        if (serverListeningSocketServerClient == null) {
            return this;
        }

        this.serverListeningSocketServerClient.registerSocketClientDelegate(new SocketClientDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                Logger.log("SocketServerClient: onConnected");
            }

            @Override
            public void onDisconnected(SocketClient client) {
                Logger.log("SocketServerClient: onDisconnected");
            }

            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                Logger.log("SocketServerClient: onResponse: " + responsePacket + "  " + responsePacket.getMessage() + "  " + Arrays.toString(responsePacket.getData()));
            }
        });
        this.serverListeningSocketServerClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {

            @Override
            public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
                Logger.log("SocketServerClient: onSendPacketBegin: " + packet + "   " + Arrays.toString(packet.getData()));
            }

            @Override
            public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
                Logger.log("SocketServerClient: onSendPacketCancel: " + packet);
            }

            @Override
            public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {
                Logger.log("SocketServerClient: onSendingPacketInProgress: " + packet + " : " + progress + " : " + sendedLength);
            }

            @Override
            public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
                Logger.log("SocketServerClient: onSendPacketEnd: " + packet);
            }

        });
        this.serverListeningSocketServerClient.registerSocketClientReceiveDelegate(new SocketClientReceivingDelegate() {
            @Override
            public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {
                Logger.log("SocketServerClient: onReceivePacketBegin: " + packet);
            }

            @Override
            public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {
                Logger.log("SocketServerClient: onReceivePacketEnd: " + packet);
            }

            @Override
            public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {
                Logger.log("SocketServerClient: onReceivePacketCancel: " + packet);
            }

            @Override
            public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {
                Logger.log("SocketServerClient: onReceivingPacketInProgress: " + packet + " : " + progress + " : " + receivedLength);
            }
        });
        return this;
    }
    protected SocketClient getServerListeningSocketServerClient() {
        return this.serverListeningSocketServerClient;
    }

    private TestClient testClient;
    protected TestClient getTestClient() {
        if (this.testClient == null) {
            this.testClient = new TestClient();
        }
        return this.testClient;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}