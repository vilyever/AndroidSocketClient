package com.vilyever.socketclient.server;

import android.support.annotation.NonNull;

import com.vilyever.socketclient.SocketClient;

import java.net.Socket;

/**
 * SocketServerClient
 * AndroidSocketClient <com.vilyever.socketclient.server>
 * Created by vilyever on 2016/3/23.
 * Feature:
 */
public class SocketServerClient extends SocketClient {
    final SocketServerClient self = this;


    /* Constructors */
    public SocketServerClient(@NonNull Socket socket) {
        super(socket.getLocalAddress().toString().substring(1), socket.getLocalPort());

        setRunningSocket(socket);

        // 此构造通常于后台线程调用，通过UIHandler确保onConnected在主线程调用
        getUiHandler().sendEmptyMessage(UIHandler.MessageType.Connected.what());
    }

    
    /* Public Methods */
    
    
    /* Properties */


    /* Overrides */


    /* Delegates */
     
     
    /* Private Methods */
    
}