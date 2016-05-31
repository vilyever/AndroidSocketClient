package com.vilyever.socketclient.server;

/**
 * SocketServerDelegate
 * Created by vilyever on 2016/5/31.
 * Feature:
 */
public interface SocketServerDelegate {
    void onServerBeginListen(SocketServer socketServer, int port);
    void onServerStopListen(SocketServer socketServer, int port);
    void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient);
    void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient);

    class SimpleSocketServerDelegate implements SocketServerDelegate {
        @Override
        public void onServerBeginListen(SocketServer socketServer, int port) {

        }

        @Override
        public void onServerStopListen(SocketServer socketServer, int port) {

        }

        @Override
        public void onClientConnected(SocketServer socketServer, SocketServerClient socketServerClient) {

        }

        @Override
        public void onClientDisconnected(SocketServer socketServer, SocketServerClient socketServerClient) {

        }
    }
}
