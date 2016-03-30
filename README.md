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
  compile 'com.github.vilyever:AndroidSocketClient:1.2.1'
}
```

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
```

## License
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
