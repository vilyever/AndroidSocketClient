# AndroidSocketClient
socket client server简易封装

## Import
[JitPack](https://jitpack.io/)

Add it in your project's build.gradle at the end of repositories:

```gradle
repositories {
  // ...
  maven { url "https://jitpack.io" }
}
```

Step 2. Add the dependency in the form

```gradle
dependencies {
  compile 'com.github.vilyever:AndroidSocketClient:1.5.0'
}
```

## Updates
* 1.5.0
</br>
重构
</br>
抽离心跳包设置到HeartBeatHelper类中
</br>
抽离包尾判断到SocketPacketHelper类中

* 1.4.1
</br>
增加禁用心跳包和超时自动断开
</br>
SocketClient.disableHeartBeat();(设置heartBeatMessage为null效果相同）
</br>
SocketClient.disableRemoteNoReplyAliveTimeout();

* 1.4.0
</br>
将发送和接收时对String和byte数组的转换移到后台线程进行

* 1.3.4
</br>
修复SocketServer停止监听时未将已连接的client断开问题
</br>
心跳包修改为可发送byte数组
</br>
自动应答集成一个帮助类

* 1.3.3
</br>
修复SocketServer启动监听回调时机问题

* 1.3.0
</br>
修改消息收发机制
</br>
接收消息回调参数由String改为SocketResponsePacket，提供byte[]数据
</br>
获取String消息调用SocketResponsePacket.getMessage()实时获取

## Usage
```java

SocketClient socketClient = new SocketClient("192.168.1.1", 80);
socketClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
    @Override
    public void onConnected(SocketClient client) {
        socketClient.sendString("message"); // 发送String消息，使用默认编码
        socketClient.sendData("message".getBytes()); // 发送byte[]消息
    }

    @Override
    public void onDisconnected(SocketClient client) {

    }

    @Override
    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
        byte[] data = responsePacket.getData(); // 获取byte[]数据
        String msg = responsePacket.getMessage(); // 使用默认编码获取String消息
    }
});

socketClient.setConnectionTimeout(1000 * 15); // 设置连接超时时长

socketClient.setCharsetName("UTF-8"); // 设置发送和接收String消息的默认编码

socketClient.getHeartBeatHelper().setSendString("heart beat"); // 设置自动发送心跳包的字符串，若为null则不发送心跳包
socketClient.getHeartBeatHelper().setSendData("heart beat".getBytes()); // 同上
socketClient.getHeartBeatHelper().setHeartBeatInterval(30 * 1000); // 设置自动发送心跳包的时间间隔，若值小于0则不发送心跳包

socketClient.getHeartBeatHelper().setReceiveString("heart beat from remote"); // 设置从远程端接收的心跳包字符串，onResponse回调将过滤此信息，若为null则不过滤
socketClient.getHeartBeatHelper().setReceiveData("heart beat from remote".getBytes()); // 同上

socketClient.getHeartBeatHelper().setRemoteNoReplyAliveTimeout(60 * 1000); // 设置远程端多长时间内没有消息发送到本地就自动断开连接，若值小于0则不自动断开

socketClient.getPollingHelper().registerQueryResponse("query", "response"); // 设置自动应答键值对，即收到"query"时自动发送"response"

socketClient.getSocketPacketHelper().setSendTailString("\r\n"); // 设置发送消息时自动在消息尾部添加的信息，远程端收到此信息后表示一条消息结束，用于解决粘包分包问题，若为null则不添加尾部信息
socketClient.getSocketPacketHelper().setSendTailData("\r\n".getBytes()); // 同上

socketClient.getSocketPacketHelper().setReceiveTailString("\r\n"); // 设置接收消息时判断消息结束的尾部信息，用于解决粘包分包问题，若为null则每次读取InputStream直到其为空，可能出现粘包问题
socketClient.getSocketPacketHelper().setReceiveTailData("\r\n".getBytes()); // 同上

socketClient.connect(); // 连接，异步进行

socketClient.disconnect(); // 断开连接

socketClient.getState(); // 获取当前状态，Connecting, Connected, Disconnected

```

## License
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
