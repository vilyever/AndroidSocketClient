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
  compile 'com.github.vilyever:AndroidSocketClient:1.2.2'
}
```

## Notice
使用SocketClient接收消息时，是通过readLine的方式进行，即每一条消息为一行，故远程端发送的消息需在末尾加上换行符 '\n '或 '\r\n'
</br>
使用SocketClient和SocketServer发送的String消息会自动在末尾添加 '\r\n' 换行符

## Usage
```java

SocketClient socketClient = new SocketClient("192.168.1.1", 80);
socketClient.registerSocketDelegate(new SocketClient.SocketDelegate() {
    @Override
    public void onConnected(SocketClient client) {

    }

    @Override
    public void onDisconnected(SocketClient client) {

    }

    @Override
    public void onResponse(SocketClient client, @NonNull String response) {

    }
});

socketClient.connect();
```

## License
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
