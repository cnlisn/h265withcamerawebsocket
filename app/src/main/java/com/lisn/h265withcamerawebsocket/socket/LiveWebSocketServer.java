package com.lisn.h265withcamerawebsocket.socket;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:17 PM
 * @desc :
 */
public class LiveWebSocketServer extends BaseWebSocket {
    String TAG = "LiveWebSocketServer";
    int PORT = 30000;


    MyWebSocketServer myWebSocketServer = new MyWebSocketServer(PORT);
    private WebSocket webSocket;

    @Override
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

    @Override
    public void start() {
        myWebSocketServer.start();
    }

    @Override
    public void release() {

        try {
            if (webSocket != null) {
                webSocket.close();
            }
            myWebSocketServer.stop();
            h265ReceiveListener = null;
            Log.e(TAG, "release: ok");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MyWebSocketServer extends WebSocketServer {

        public MyWebSocketServer(int port) {
            super(new InetSocketAddress(port));
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            webSocket = conn;
            Log.e(TAG, "onOpen: ");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.e(TAG, "onClose: ");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.e(TAG, "onMessage: " + message);
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            super.onMessage(conn, message);
            if (h265ReceiveListener != null) {
                byte[] buf = new byte[message.remaining()];
                message.get(buf);
                Log.i(TAG, "onMessage:" + buf.length);
                h265ReceiveListener.onReceive(buf);
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.e(TAG, "onError: ", ex);
        }

        @Override
        public void onStart() {
            Log.e(TAG, "onStart: ");
        }
    }
}
