package com.vilyever.socketclient;

import com.vilyever.socketclient.util.BytesWrapper;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * PollingHelper
 * AndroidSocketClient <com.vilyever.socketclient>
 * Created by vilyever on 2016/4/27.
 * Feature:
 */
public class PollingHelper {
    final PollingHelper self = this;

    
    /* Constructors */
    public PollingHelper(String defaultCharsetName) {
        this.defaultCharsetName = defaultCharsetName;

        registerQueryResponse(SocketPacket.DefaultPollingQueryMessage, SocketPacket.DefaultPollingResponseMessage);
    }

    
    /* Public Methods */
    public PollingHelper registerQueryResponse(String query, String response) {
        return registerQueryResponse(query, response, getDefaultCharsetName());
    }

    public PollingHelper registerQueryResponse(String query, String response, String charsetName) {
        return registerQueryResponse(query.getBytes(Charset.forName(charsetName)), response.getBytes(Charset.forName(charsetName)));
    }

    public PollingHelper registerQueryResponse(byte[] query, byte[] response) {
        getQueryResponseMap().put(new BytesWrapper(query), new BytesWrapper(response));
        return this;
    }

    public PollingHelper registerQueryResponse(HashMap<BytesWrapper, BytesWrapper> queryResponseMap) {
        getQueryResponseMap().putAll(queryResponseMap);
        return this;
    }

    public PollingHelper removeQueryResponse(String query) {
        return removeQueryResponse(query, getDefaultCharsetName());
    }

    public PollingHelper removeQueryResponse(String query, String charsetName) {
        return removeQueryResponse(query.getBytes(Charset.forName(charsetName)));
    }

    public PollingHelper removeQueryResponse(byte[] query) {
        getQueryResponseMap().remove(query);
        return this;
    }

    public PollingHelper clear() {
        getQueryResponseMap().clear();
        return this;
    }

    public PollingHelper append(PollingHelper pollingHelper) {
        registerQueryResponse(pollingHelper.getQueryResponseMap());
        return this;
    }

    public boolean containsQuery(byte[] bytes) {
        for (Map.Entry<BytesWrapper, BytesWrapper> entry : getQueryResponseMap().entrySet()) {
            if (entry.getKey().equalsBytes(bytes)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsResponse(byte[] bytes) {
        for (Map.Entry<BytesWrapper, BytesWrapper> entry : getQueryResponseMap().entrySet()) {
            if (entry.getValue().equalsBytes(bytes)) {
                return true;
            }
        }
        return false;
    }

    public byte[] getResponse(byte[] query) {
        return getResponse(new BytesWrapper(query)).getBytes();
    }

    public BytesWrapper getResponse(BytesWrapper bytesWrapper) {
        return getQueryResponseMap().get(bytesWrapper);
    }

    /* Properties */
    private final String defaultCharsetName;
    public String getDefaultCharsetName() {
        return this.defaultCharsetName;
    }

    private HashMap<BytesWrapper, BytesWrapper> queryResponseMap;
    protected HashMap<BytesWrapper, BytesWrapper> getQueryResponseMap() {
        if (this.queryResponseMap == null) {
            this.queryResponseMap = new HashMap<>();
        }
        return this.queryResponseMap;
    }
    
    /* Overrides */
    
    
    /* Delegates */
    
    
    /* Private Methods */
    
}