//package com.vilyever.socketclient.helper;
//
//import com.vilyever.socketclient.util.BytesWrapper;
//import com.vilyever.socketclient.util.CharsetUtil;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * PollingHelper
// * AndroidSocketClient <com.vilyever.socketclient>
// * Created by vilyever on 2016/4/27.
// * Feature:
// */
//public class PollingHelper {
//    final PollingHelper self = this;
//
//
//    /* Constructors */
//    public PollingHelper(String charsetName) {
//        this.charsetName = charsetName;
//    }
//
//
//    /* Public Methods */
//    public PollingHelper registerQueryResponse(String query, String response) {
//        if (query == null || response == null) {
//            return this;
//        }
//        return registerQueryResponse(CharsetUtil.stringToData(query, getCharsetName()), CharsetUtil.stringToData(response, getCharsetName()));
//    }
//
//    /**
//     * 注册自动应答
//     * @param query 接收
//     * @param response 自动回复
//     * @return
//     */
//    public PollingHelper registerQueryResponse(byte[] query, byte[] response) {
//        if (query == null || response == null) {
//            return this;
//        }
//        getQueryResponseMap().put(new BytesWrapper(query), new BytesWrapper(response));
//        return this;
//    }
//
//    public PollingHelper registerQueryResponse(HashMap<BytesWrapper, BytesWrapper> queryResponseMap) {
//        if (queryResponseMap == null) {
//            return this;
//        }
//        getQueryResponseMap().putAll(queryResponseMap);
//        return this;
//    }
//
//    public PollingHelper removeQueryResponse(String query) {
//        if (query == null) {
//            return this;
//        }
//        return removeQueryResponse(CharsetUtil.stringToData(query, getCharsetName()));
//    }
//
//    public PollingHelper removeQueryResponse(byte[] query) {
//        if (query == null) {
//            return this;
//        }
//        getQueryResponseMap().remove(query);
//        return this;
//    }
//
//    public PollingHelper clear() {
//        getQueryResponseMap().clear();
//        return this;
//    }
//
//    public PollingHelper append(PollingHelper pollingHelper) {
//        if (pollingHelper == null) {
//            return this;
//        }
//        registerQueryResponse(pollingHelper.getQueryResponseMap());
//        return this;
//    }
//
//    public boolean containsQuery(byte[] data) {
//        if (data == null) {
//            return false;
//        }
//        for (Map.Entry<BytesWrapper, BytesWrapper> entry : getQueryResponseMap().entrySet()) {
//            if (entry.getKey().equalsBytes(data)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean containsResponse(byte[] data) {
//        if (data == null) {
//            return false;
//        }
//        for (Map.Entry<BytesWrapper, BytesWrapper> entry : getQueryResponseMap().entrySet()) {
//            if (entry.getValue().equalsBytes(data)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public byte[] getResponse(byte[] query) {
//        if (query == null) {
//            return null;
//        }
//        return getResponse(new BytesWrapper(query)).getBytes();
//    }
//
//    public BytesWrapper getResponse(BytesWrapper bytesWrapper) {
//        if (bytesWrapper == null) {
//            return null;
//        }
//        return getQueryResponseMap().get(bytesWrapper);
//    }
//
//    /* Properties */
//    private String charsetName;
//    public PollingHelper setCharsetName(String charsetName) {
//        this.charsetName = charsetName;
//        return this;
//    }
//    public String getCharsetName() {
//        return this.charsetName;
//    }
//
//    private HashMap<BytesWrapper, BytesWrapper> queryResponseMap;
//    protected HashMap<BytesWrapper, BytesWrapper> getQueryResponseMap() {
//        if (this.queryResponseMap == null) {
//            this.queryResponseMap = new HashMap<>();
//        }
//        return this.queryResponseMap;
//    }
//
//    /* Overrides */
//
//
//    /* Delegates */
//
//
//    /* Private Methods */
//
//}