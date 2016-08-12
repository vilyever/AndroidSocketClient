package com.vilyever.socketclient.helper;

import com.vilyever.socketclient.SocketClient;

/**
 * SocketClientSendingDelegate
 * Created by vilyever on 2016/5/30.
 * Feature:
 */
public interface SocketClientSendingDelegate {
    void onSendPacketBegin(SocketClient client, SocketPacket packet);
    void onSendPacketEnd(SocketClient client, SocketPacket packet);
    void onSendPacketCancel(SocketClient client, SocketPacket packet);

    /**
     * 发送进度回调
     * @param client
     * @param packet 正在发送的packet
     * @param progress 0.0f-1.0f
     * @param sendedLength 已发送的字节数
     */
    void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength);

    class SimpleSocketClientSendingDelegate implements SocketClientSendingDelegate {
        @Override
        public void onSendPacketBegin(SocketClient client, SocketPacket packet) {

        }

        @Override
        public void onSendPacketEnd(SocketClient client, SocketPacket packet) {

        }

        @Override
        public void onSendPacketCancel(SocketClient client, SocketPacket packet) {

        }

        @Override
        public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {

        }
    }
}
