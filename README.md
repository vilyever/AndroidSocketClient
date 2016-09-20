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
  compile 'com.github.vilyever:AndroidSocketClient:3.0.2'
}
```

## Updates
* 3.0.2
</br>
修复初次连接失败时，因CountDownTimer导致崩溃的问题 [issue #24](/../../issues/24)

* 3.0.1
</br>
修复包头验证bug，by zzdwuliang
</br>
增加地址检测的详细log
</br>

* 3.0.0
</br>
支持ReadToData和ReadToLength自动读取以下两种结构
</br>
常见包结构1：【包头（可选）】【正文】【包尾】
</br>
常见包结构2：【包头（可选）】【余下包长度（正文加包尾长度）（此部分也可做包头）（此部分长度固定）】【正文】【包尾（可选）】

## Usage
### app模块下包含简单的使用demo
### 请一定设置读取策略socketClient.getSocketPacketHelper().setReadStrategy();

### 远程端连接信息配置
```java
    socketClient.getAddress().setRemoteIP(IPUtil.getLocalIPAddress(true)); // 远程端IP地址
    socketClient.getAddress().setRemotePort("21998"); // 远程端端口号
    socketClient.getAddress().setConnectionTimeout(15 * 1000); // 连接超时时长，单位毫秒
```

### 默认String编码配置
```java
    /**
     * 设置自动转换String类型到byte[]类型的编码
     * 如未设置（默认为null），将不能使用{@link SocketClient#sendString(String)}发送消息
     * 如设置为非null（如UTF-8），在接受消息时会自动尝试在接收线程（非主线程）将接收的byte[]数据依照编码转换为String，在{@link SocketResponsePacket#getMessage()}读取
     */
    socketClient.setCharsetName(CharsetUtil.UTF_8); // 设置编码为UTF-8
```

### 固定心跳包配置
```java
    /**
     * 设置自动发送的心跳包信息
     */
    socketClient.getHeartBeatHelper().setDefaultSendData(CharsetUtil.stringToData("HeartBeat", CharsetUtil.UTF_8));

    /**
     * 设置远程端发送到本地的心跳包信息内容，用于判断接收到的数据包是否是心跳包
     * 通过{@link SocketResponsePacket#isHeartBeat()} 查看数据包是否是心跳包
     */
    socketClient.getHeartBeatHelper().setDefaultReceiveData(CharsetUtil.stringToData("HeartBeat", CharsetUtil.UTF_8));

    socketClient.getHeartBeatHelper().setHeartBeatInterval(10 * 1000); // 设置自动发送心跳包的间隔时长，单位毫秒
    socketClient.getHeartBeatHelper().setSendHeartBeatEnabled(true); // 设置允许自动发送心跳包，此值默认为false
```

### 动态变化心跳包配置
```java
    /**
     * 设置自动发送的心跳包信息
     * 此信息动态生成
     *
     * 每次发送心跳包时自动调用
     */
    socketClient.getHeartBeatHelper().setSendDataBuilder(new SocketHeartBeatHelper.SendDataBuilder() {
        @Override
        public byte[] obtainSendHeartBeatData(SocketHeartBeatHelper helper) {
            /**
             * 使用当前日期作为心跳包
             */
            byte[] heartBeatPrefix = new byte[]{0x1F, 0x1F};
            byte[] heartBeatSuffix = new byte[]{0x1F, 0x1F};

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            byte[] heartBeatInfo = CharsetUtil.stringToData(sdf.format(new Date()), CharsetUtil.UTF_8);

            byte[] data = new byte[heartBeatPrefix.length + heartBeatSuffix.length + heartBeatInfo.length];
            System.arraycopy(heartBeatPrefix, 0, data, 0, heartBeatPrefix.length);
            System.arraycopy(heartBeatInfo, 0, data, heartBeatPrefix.length, heartBeatInfo.length);
            System.arraycopy(heartBeatSuffix, 0, data, heartBeatPrefix.length + heartBeatInfo.length, heartBeatSuffix.length);

            return data;
        }
    });

    /**
     * 设置远程端发送到本地的心跳包信息的检测器，用于判断接收到的数据包是否是心跳包
     * 通过{@link SocketResponsePacket#isHeartBeat()} 查看数据包是否是心跳包
     */
    socketClient.getHeartBeatHelper().setReceiveHeartBeatPacketChecker(new SocketHeartBeatHelper.ReceiveHeartBeatPacketChecker() {
        @Override
        public boolean isReceiveHeartBeatPacket(SocketHeartBeatHelper helper, SocketResponsePacket packet) {
            /**
             * 判断数据包信息是否含有指定的心跳包前缀和后缀
             */
            byte[] heartBeatPrefix = new byte[]{0x1F, 0x1F};
            byte[] heartBeatSuffix = new byte[]{0x1F, 0x1F};

            if (Arrays.equals(heartBeatPrefix, Arrays.copyOfRange(packet.getData(), 0, heartBeatPrefix.length))
                    && Arrays.equals(heartBeatSuffix, Arrays.copyOfRange(packet.getData(), packet.getData().length - heartBeatSuffix.length, packet.getData().length))) {
                return true;
            }

            return false;
        }
    });

    socketClient.getHeartBeatHelper().setHeartBeatInterval(10 * 1000); // 设置自动发送心跳包的间隔时长，单位毫秒
    socketClient.getHeartBeatHelper().setSendHeartBeatEnabled(true); // 设置允许自动发送心跳包，此值默认为false
```

### 自动按包尾分割信息读取数据的发送配置
```java
    /**
     * 根据连接双方协议设置自动发送的包尾数据
     * 每次发送数据包（包括心跳包）都会在发送包内容后自动发送此包尾
     *
     * 例：socketClient.sendData(new byte[]{0x01, 0x02})的步骤为
     * 1. socketClient向远程端发送包头（如果设置了包头信息）
     * 2. socketClient向远程端发送正文数据{0x01, 0x02}
     * 3. socketClient向远程端发送包尾
     *
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadToTrailer}必须设置此项
     * 用于分隔多条消息
     */
    socketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{0x13, 0x10});

    /**
     * 根据连接双方协议设置自动发送的包头数据
     * 每次发送数据包（包括心跳包）都会在发送包内容前自动发送此包头
     *
     * 若无需包头可删除此行
     */
    socketClient.getSocketPacketHelper().setSendHeaderData(CharsetUtil.stringToData("SocketClient:", CharsetUtil.UTF_8));

    /**
     * 设置分段发送数据长度
     * 即在发送指定长度后通过 {@link SocketClientSendingDelegate#onSendingPacketInProgress(SocketClient, SocketPacket, float, int)}回调当前发送进度
     *
     * 若无需进度回调可删除此二行，删除后仍有【发送开始】【发送结束】的回调
     */
    socketClient.getSocketPacketHelper().setSendSegmentLength(8); // 设置发送分段长度，单位byte
    socketClient.getSocketPacketHelper().setSendSegmentEnabled(true); // 设置允许使用分段发送，此值默认为false

    /**
     * 设置发送超时时长
     * 在发送每个数据包时，发送每段数据的最长时间，超过后自动断开socket连接
     * 通过设置分段发送{@link SocketPacketHelper#setSendSegmentEnabled(boolean)} 可避免发送大数据包时因超时断开，
     *
     * 若无需限制发送时长可删除此二行
     */
    socketClient.getSocketPacketHelper().setSendTimeout(30 * 1000); // 设置发送超时时长，单位毫秒
    socketClient.getSocketPacketHelper().setSendTimeoutEnabled(true); // 设置允许使用发送超时时长，此值默认为false
```

### 自动按包尾分割信息读取数据的接收配置
```java
    /**
     * 设置读取策略为自动读取到指定的包尾
     */
    socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);

    /**
     * 根据连接双方协议设置的包尾数据
     * 每次接收数据包（包括心跳包）都会在检测接收到与包尾数据相同的byte[]时回调一个数据包
     *
     * 例：自动接收远程端所发送的socketClient.sendData(new byte[]{0x01, 0x02})【{0x01, 0x02}为将要接收的数据】的步骤为
     * 1. socketClient接收包头（如果设置了包头信息）（接收方式为一直读取到与包头相同的byte[],即可能过滤掉包头前的多余信息）
     * 2. socketClient接收正文和包尾（接收方式为一直读取到与尾相同的byte[]）
     * 3. socketClient回调数据包
     *
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadToTrailer}必须设置此项
     * 用于分隔多条消息
     */
    socketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x13, 0x10});

    /**
     * 根据连接双方协议设置的包头数据
     * 每次接收数据包（包括心跳包）都会先接收此包头
     *
     * 若无需包头可删除此行
     */
    socketClient.getSocketPacketHelper().setReceiveHeaderData(CharsetUtil.stringToData("SocketClient:", CharsetUtil.UTF_8));

    /**
     * 设置接收超时时长
     * 在指定时长内没有数据到达本地自动断开
     *
     * 若无需限制接收时长可删除此二行
     */
    socketClient.getSocketPacketHelper().setReceiveTimeout(120 * 1000); // 设置接收超时时长，单位毫秒
    socketClient.getSocketPacketHelper().setReceiveTimeoutEnabled(true); // 设置允许使用接收超时时长，此值默认为false
```

### 自动按包长度信息读取的发送配置
```java
    /**
     * 设置包长度转换器
     * 即每次发送数据时，将包头以外的数据长度转换为特定的byte[]发送到远程端用于解析还需要读取多少长度的数据
     *
     * 例：socketClient.sendData(new byte[]{0x01, 0x02})的步骤为
     * 1. socketClient向远程端发送包头（如果设置了包头信息）
     * 2. socketClient要发送的数据为{0x01, 0x02}，长度为2（若设置了包尾，还需加上包尾的字节长度），通过此转换器将int类型的2转换为4字节的byte[]，远程端也照此算法将4字节的byte[]转换为int值
     * 3. socketClient向远程端发送转换后的长度信息byte[]
     * 4. socketClient向远程端发送正文数据{0x01, 0x02}
     * 5. socketClient向远程端发送包尾（如果设置了包尾信息）
     *
     * 此转换器用于第二步
     *
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadByLength}必须设置此项
     * 用于分隔多条消息
     */
    socketClient.getSocketPacketHelper().setSendPacketLengthDataConvertor(new SocketPacketHelper.SendPacketLengthDataConvertor() {
        @Override
        public byte[] obtainSendPacketLengthDataForPacketLength(SocketPacketHelper helper, int packetLength) {
            /**
             * 简单将int转换为byte[]
             */
            byte[] data = new byte[4];
            data[3] = (byte) (packetLength & 0xFF);
            data[2] = (byte) ((packetLength >> 8) & 0xFF);
            data[1] = (byte) ((packetLength >> 16) & 0xFF);
            data[0] = (byte) ((packetLength >> 24) & 0xFF);
            return data;
        }
    });

    /**
     * 根据连接双方协议设置自动发送的包头数据
     * 每次发送数据包（包括心跳包）都会在发送包内容前自动发送此包头
     *
     * 若无需包头可删除此行
     */
    socketClient.getSocketPacketHelper().setSendHeaderData(CharsetUtil.stringToData("SocketClient:", CharsetUtil.UTF_8));

    /**
     * 根据连接双方协议设置自动发送的包尾数据
     * 每次发送数据包（包括心跳包）都会在发送包内容后自动发送此包尾
     *
     * 若无需包尾可删除此行
     * 注意：
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadByLength}时不依赖包尾读取数据
     */
    socketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{0x13, 0x10});

    /**
     * 设置分段发送数据长度
     * 即在发送指定长度后通过 {@link SocketClientSendingDelegate#onSendingPacketInProgress(SocketClient, SocketPacket, float, int)}回调当前发送进度
     *
     * 若无需进度回调可删除此二行，删除后仍有【发送开始】【发送结束】的回调
     */
    socketClient.getSocketPacketHelper().setSendSegmentLength(8); // 设置发送分段长度，单位byte
    socketClient.getSocketPacketHelper().setSendSegmentEnabled(true); // 设置允许使用分段发送，此值默认为false

    /**
     * 设置发送超时时长
     * 在发送每个数据包时，发送每段数据的最长时间，超过后自动断开socket连接
     * 通过设置分段发送{@link SocketPacketHelper#setSendSegmentEnabled(boolean)} 可避免发送大数据包时因超时断开，
     *
     * 若无需限制发送时长可删除此二行
     */
    socketClient.getSocketPacketHelper().setSendTimeout(30 * 1000); // 设置发送超时时长，单位毫秒
    socketClient.getSocketPacketHelper().setSendTimeoutEnabled(true); // 设置允许使用发送超时时长，此值默认为false
```

### 自动按包长度信息读取的接收配置
```java
    /**
     * 设置读取策略为自动读取指定长度
     */
    socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadByLength);

    /**
     * 设置包长度转换器
     * 即每次接收数据时，将远程端发送到本地的长度信息byte[]转换为int，然后读取相应长度的值
     *
     * 例：自动接收远程端所发送的socketClient.sendData(new byte[]{0x01, 0x02})【{0x01, 0x02}为将要接收的数据】的步骤为
     * 1. socketClient接收包头（如果设置了包头信息）（接收方式为一直读取到与包头相同的byte[],即可能过滤掉包头前的多余信息）
     * 2. socketClient接收长度为{@link SocketPacketHelper#getReceivePacketLengthDataLength()}（此处设置为4）的byte[]，通过下面设置的转换器，将byte[]转换为int值，此int值暂时称为X
     * 3. socketClient接收长度为X的byte[]
     * 4. socketClient接收包尾（如果设置了包尾信息）（接收方式为一直读取到与包尾相同的byte[],如无意外情况，此处不会读取到多余的信息）
     * 5. socketClient回调数据包
     *
     * 此转换器用于第二步
     *
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadByLength}必须设置此项
     * 用于分隔多条消息
     */
    socketClient.getSocketPacketHelper().setReceivePacketLengthDataLength(4);
    socketClient.getSocketPacketHelper().setReceivePacketDataLengthConvertor(new SocketPacketHelper.ReceivePacketDataLengthConvertor() {
        @Override
        public int obtainReceivePacketDataLength(SocketPacketHelper helper, byte[] packetLengthData) {
            /**
             * 简单将byte[]转换为int
             */
            int length =  (packetLengthData[3] & 0xFF) + ((packetLengthData[2] & 0xFF) << 8) + ((packetLengthData[1] & 0xFF) << 16) + ((packetLengthData[0] & 0xFF) << 24);

            return length;
        }
    });

    /**
     * 根据连接双方协议设置的包头数据
     * 每次接收数据包（包括心跳包）都会先接收此包头
     *
     * 若无需包头可删除此行
     */
    socketClient.getSocketPacketHelper().setReceiveHeaderData(CharsetUtil.stringToData("SocketClient:", CharsetUtil.UTF_8));

    /**
     * 根据连接双方协议设置的包尾数据
     *
     * 若无需包尾可删除此行
     * 注意：
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadByLength}时不依赖包尾读取数据
     */
    socketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{0x13, 0x10});

    /**
     * 设置接收超时时长
     * 在指定时长内没有数据到达本地自动断开
     *
     * 若无需限制接收时长可删除此二行
     */
    socketClient.getSocketPacketHelper().setReceiveTimeout(120 * 1000); // 设置接收超时时长，单位毫秒
    socketClient.getSocketPacketHelper().setReceiveTimeoutEnabled(true); // 设置允许使用接收超时时长，此值默认为false
```

### 手动读取的发送配置
```java
    /**
     * 设置分段发送数据长度
     * 即在发送指定长度后通过 {@link SocketClientSendingDelegate#onSendingPacketInProgress(SocketClient, SocketPacket, float, int)}回调当前发送进度
     *
     * 若无需进度回调可删除此二行，删除后仍有【发送开始】【发送结束】的回调
     */
    socketClient.getSocketPacketHelper().setSendSegmentLength(8); // 设置发送分段长度，单位byte
    socketClient.getSocketPacketHelper().setSendSegmentEnabled(true); // 设置允许使用分段发送，此值默认为false

    /**
     * 设置发送超时时长
     * 在发送每个数据包时，发送每段数据的最长时间，超过后自动断开socket连接
     * 通过设置分段发送{@link SocketPacketHelper#setSendSegmentEnabled(boolean)} 可避免发送大数据包时因超时断开，
     *
     * 若无需限制发送时长可删除此二行
     */
    socketClient.getSocketPacketHelper().setSendTimeout(30 * 1000); // 设置发送超时时长，单位毫秒
    socketClient.getSocketPacketHelper().setSendTimeoutEnabled(true); // 设置允许使用发送超时时长，此值默认为false
```

### 手动读取的接收配置
```java
    /**
     * 设置读取策略为手动读取
     * 手动读取有两种方法
     * 1. {@link SocketClient#readDataToData(byte[], boolean)} )} 读取到与指定字节相同的字节序列后回调数据包
     * 2. {@link SocketClient#readDataToLength(int)} 读取指定长度的字节后回调数据包
     *
     * 此时SocketPacketHelper中其他读取相关设置将会无效化
     */
    socketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.Manually);
```

### 常用回调配置
```java
    // 对应removeSocketClientDelegate
    socketClient.registerSocketClientDelegate(new SocketClientDelegate() {
        /**
         * 连接上远程端时的回调
         */
        @Override
        public void onConnected(SocketClient client) {
             SocketPacket packet = socketClient.sendData(new byte[]{0x02}); // 发送消息
             packet = socketClient.sendString("sy hi!"); // 发送消息

             socketClient.cancelSend(packet); // 取消发送，若在等待发送队列中则从队列中移除，若正在发送则无法取消
        }

        /**
         * 与远程端断开连接时的回调
         */
        @Override
        public void onDisconnected(SocketClient client) {
            // 可在此实现自动重连
            socketClient.connect();
        }

        /**
         * 接收到数据包时的回调
         */
        @Override
        public void onResponse(final SocketClient client, @NonNull SocketResponsePacket responsePacket) {
            byte[] data = responsePacket.getData(); // 获取接收的byte数组，不为null
            String message = responsePacket.getMessage(); // 获取按默认设置的编码转化的String，可能为null
        }
    });
```

### 发送状态回调配置
```java
    // 对应removeSocketClientSendingDelegate
    socketClient.registerSocketClientSendingDelegate(new SocketClientSendingDelegate() {
        /**
         * 数据包开始发送时的回调
         */
        @Override
        public void onSendPacketBegin(SocketClient client, SocketPacket packet) {
        }

        /**
         * 数据包取消发送时的回调
         * 取消发送回调有以下情况：
         * 1. 手动cancel仍在排队，还未发送过的packet
         * 2. 断开连接时，正在发送的packet和所有在排队的packet都会被取消
         */
        @Override
        public void onSendPacketCancel(SocketClient client, SocketPacket packet) {
        }

        /**
         * 数据包发送的进度回调
         * progress值为[0.0f, 1.0f]
         * 通常配合分段发送使用
         * 可用于显示文件等大数据的发送进度
         */
        @Override
        public void onSendingPacketInProgress(SocketClient client, SocketPacket packet, float progress, int sendedLength) {
        }

        /**
         * 数据包完成发送时的回调
         */
        @Override
        public void onSendPacketEnd(SocketClient client, SocketPacket packet) {
        }
    });
```

### 接收状态回调配置
```java
    // 对应removeSocketClientReceiveDelegate
    socketClient.registerSocketClientReceiveDelegate(new SocketClientReceivingDelegate() {
        /**
         * 开始接受一个新的数据包时的回调
         */
        @Override
        public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {
        }

        /**
         * 完成接受一个新的数据包时的回调
         */
        @Override
        public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {
        }

        /**
         * 取消接受一个新的数据包时的回调
         * 在断开连接时会触发
         */
        @Override
        public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {
        }

        /**
         * 接受一个新的数据包的进度回调
         * progress值为[0.0f, 1.0f]
         * 仅作用于ReadStrategy为AutoReadByLength的自动读取
         * 因AutoReadByLength可以首先接受到剩下的数据包长度
         */
        @Override
        public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {
        }
    });
```

## License
[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
