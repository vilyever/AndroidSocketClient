package com.vilyever.androidsocketclient;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.vilyever.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientAddress;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketClientSendingDelegate;
import com.vilyever.socketclient.helper.SocketPacket;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;
import com.vilyever.socketclient.util.IPUtil;

import java.util.Arrays;

/**
 * TestClient
 * Created by vilyever on 2016/7/26.
 * Feature:
 */
public class TestClient {
    final TestClient self = this;
    
    
    /* Constructors */
    
    
    /* Public Methods */
    public void connect() {
        self.getLocalSocketClient().connect();
    }
    
    /* Properties */
    private SocketClient localSocketClient;
    public SocketClient getLocalSocketClient() {
        if (this.localSocketClient == null) {
            this.localSocketClient = new SocketClient();
            this.localSocketClient.getAddress().setRemoteIP("");
            this.localSocketClient.getAddress().setRemotePort("");
            this.localSocketClient.getAddress().setConnectionTimeout("");
            this.localSocketClient.setCharsetName(CharsetUtil.UTF_8);

            this.localSocketClient.getSocketPacketHelper().setSendHeaderData(CharsetUtil.stringToData("Local:", CharsetUtil.UTF_8));
            this.localSocketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{0x13, 0x10});
            this.localSocketClient.getSocketPacketHelper().setSendPacketLengthDataConvertor(new SocketPacketHelper.SendPacketLengthDataConvertor() {
                @Override
                public byte[] obtainSendPacketLengthDataForPacketLength(SocketPacketHelper helper, int packetLength) {
                    byte[] ret = new byte[4];
                    ret[3] = (byte) (packetLength & 0xFF);
                    ret[2] = (byte) ((packetLength >> 8) & 0xFF);
                    ret[1] = (byte) ((packetLength >> 16) & 0xFF);
                    ret[0] = (byte) ((packetLength >> 24) & 0xFF);
                    Log.d("logger", "packetLength " + packetLength + "   , ret " + Arrays.toString(ret));
                    return ret;
                }
            });
            this.localSocketClient.getSocketPacketHelper().setSendSegmentLength(1);
            this.localSocketClient.getSocketPacketHelper().setSendSegmentEnabled(true);

//            this.localSocketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
            this.localSocketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x13, 0x10});

            this.localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
                @Override
                public void onConnected(SocketClient client) {
                    Logger.log("SocketClient: onConnected");
                    client.sendString("from android client: hello");
                    client.readDataToLength(26);
                }

                @Override
                public void onDisconnected(SocketClient client) {
                    Logger.log("SocketClient: onDisconnected");
                }

                @Override
                public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                    Logger.log("SocketClient: onResponse: " + responsePacket + "  " + responsePacket.getMessage() + "  " + Arrays.toString(responsePacket.getData()));
                }
            });
            this.localSocketClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {

                @Override
                public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
                    Logger.log("SocketClient: onSendPacketBegin: " + packet + "   " + Arrays.toString(packet.getData()));
                }

                @Override
                public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
                    Logger.log("SocketClient: onSendPacketCancel: " + packet);
                }

                @Override
                public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {
                    Logger.log("SocketClient: onSendingPacketInProgress: " + packet + " : " + progress + " : " + sendedLength);
                }

                @Override
                public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
                    Logger.log("SocketClient: onSendPacketEnd: " + packet);
                }
            });
            this.localSocketClient.registerSocketClientReceiveDelegate(new SocketClientReceivingDelegate() {
                @Override
                public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {
                    Logger.log("SocketClient: onReceivePacketBegin: " + packet);
                }

                @Override
                public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {
                    Logger.log("SocketClient: onReceivePacketEnd: " + packet);
                }

                @Override
                public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {
                    Logger.log("SocketClient: onReceivePacketCancel: " + packet);
                }

                @Override
                public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {
                    Logger.log("SocketClient: onReceivingPacketInProgress: " + packet + " : " + progress + " : " + receivedLength);
                }
            });
        }
        return this.localSocketClient;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    private void __i__setupAddress(SocketClient socketClient) {
        socketClient.getAddress().setRemoteIP(IPUtil.getLocalIPAddress(true));
        socketClient.getAddress().setRemotePort("21998");
        socketClient.getAddress().setConnectionTimeout(30 * 1000);
    }

    private void __i__setupEncoding(SocketClient socketClient) {
        socketClient.setCharsetName(CharsetUtil.UTF_8);
    }

    private void __i__setupSendHeader(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setSendHeaderData(CharsetUtil.stringToData("Local:", CharsetUtil.UTF_8));
    }

    private void __i__setupSendTrailer(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{0x13, 0x10});
    }

    private void __i__setupSendSegment(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setSendSegmentLength(8);
        socketClient.getSocketPacketHelper().setSendSegmentEnabled(true);
    }


    private void __i__setupSendLength(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setSendPacketLengthDataConvertor(new SocketPacketHelper.SendPacketLengthDataConvertor() {
            @Override
            public byte[] obtainSendPacketLengthDataForPacketLength(SocketPacketHelper helper, int packetLength) {
                byte[] ret = new byte[4];
                ret[3] = (byte) (packetLength & 0xFF);
                ret[2] = (byte) ((packetLength >> 8) & 0xFF);
                ret[1] = (byte) ((packetLength >> 16) & 0xFF);
                ret[0] = (byte) ((packetLength >> 24) & 0xFF);
                return ret;
            }
        });
    }

    private void __i__setupReceiveHeader(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setReceiveHeaderData(CharsetUtil.stringToData("Server:", CharsetUtil.UTF_8));
    }

    private void __i__setupReceiveByTrailer(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
        socketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x13, 0x10});
    }

    private void __i__setupReceiveByLength(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadByLength);
        socketClient.getSocketPacketHelper().setReceivePacketLengthDataLength(4);
        socketClient.getSocketPacketHelper().setReceivePacketDataLengthConvertor(new SocketPacketHelper.ReceivePacketDataLengthConvertor() {
            @Override
            public int obtainReceivePacketDataLength(SocketPacketHelper helper, byte[] packetLengthData) {
                int length =  (packetLengthData[3] & 0xFF) + ((packetLengthData[2] & 0xFF) << 8) + ((packetLengthData[1] & 0xFF) << 16) + ((packetLengthData[0] & 0xFF) << 24);

                return length;
            }
        });
    }

    private void __i__setupReceiveByManually(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.Manually);
    }
}