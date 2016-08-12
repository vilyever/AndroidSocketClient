package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.SocketClient;

/**
 * SocketClientReceivingDelegate
 * Created by vilyever on 2016/5/30.
 * Feature:
 */
public interface SocketClientReceivingDelegate {
    void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet);
    void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet);
    void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet);
    void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength);

    class SimpleSocketClientReceiveDelegate implements SocketClientReceivingDelegate {
        @Override
        public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {

        }

        @Override
        public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {

        }

        @Override
        public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {

        }

        @Override
        public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {

        }
    }
}
