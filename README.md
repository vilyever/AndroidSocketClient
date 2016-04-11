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
  compile 'com.github.vilyever:AndroidSocketClient:1.3.0'
}
```

## Updates
* 1.3.0
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
        socketClient.send("message"); // 发送String消息，使用默认编码
        socketClient.send("message", "GBK"); // 发送String消息，使用GBK编码
        socketClient.send("message".getBytes()); // 发送byte[]消息
    }

    @Override
    public void onDisconnected(SocketClient client) {

    }

    @Override
    public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {

    }
});

socketClient.setConnectionTimeout(1000 * 15); // 设置连接超时时长

socketClient.setHeartBeatInterval(1000 * 30); // 设置心跳包发送间隔

socketClient.setRemoteNoReplyAliveTimeout(1000 * 60); // 设置远程端在多长时间没有消息发送到本地时自动断开连接

socketClient.registerQueryResponse("query", "response"); // 设置自动应答键值对，即收到"query"时自动发送"response"

socketClient.setSupportReadLine(false); // 设置是否支持对每条消息添加换行符分割，默认为true

socketClient.setCharsetName("UTF-8"); // 设置发送String消息的默认编码

socketClient.connect(); // 连接，异步进行

socketClient.disconnect(); // 断开连接

socketClient.getState(); // 获取当前状态，Connecting, Connected, Disconnected

```

## License
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
