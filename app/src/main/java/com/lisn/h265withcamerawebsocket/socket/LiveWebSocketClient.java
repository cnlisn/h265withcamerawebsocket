package com.lisn.h265withcamerawebsocket.socket;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:18 PM
 * @desc :
 */
public class LiveWebSocketClient extends BaseWebSocket {

    String TAG = "LiveWebSocketClient";

    static {
        int PORT = 30000;

        // 另一台手机的IP，如果自己测试自己改下哦
        url = "ws://172.16.20.206:" + PORT;
    }

    private static String url;
    private MyWebSocketClient myWebSocketClient;

    @Override
    public void sendData(byte[] bytes) {
        if (myWebSocketClient != null && myWebSocketClient.isOpen()) {
            myWebSocketClient.send(bytes);
        }
    }

    @Override
    public void start() {
        try {
            URI uri = new URI(url);
            myWebSocketClient = new MyWebSocketClient(uri);
            myWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void release() {
        try {
            if (myWebSocketClient != null) {
                myWebSocketClient.close();
                h265ReceiveListener = null;
                Log.e(TAG, "release: ok");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MyWebSocketClient extends WebSocketClient {
        public MyWebSocketClient(URI uri) {
            super(uri);
            Log.e(TAG, "MyWebSocketClient: uri= " + uri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.e(TAG, "onOpen: ");
        }

        @Override
        public void onMessage(String message) {
            Log.e(TAG, "onMessage: " + message);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            if (h265ReceiveListener != null) {
                byte[] buf = new byte[bytes.remaining()];
                bytes.get(buf);
                Log.i(TAG, "onMessage:" + buf.length);
                h265ReceiveListener.onReceive(buf);
            }

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e(TAG, "onClose: reason=" + reason + " code=" + code);
        }

        @Override
        public void onError(Exception ex) {
            Log.e(TAG, "onError: ", ex);
        }
    }
}
