package com.lisn.h265withcamerawebsocket;

import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.lisn.h265withcamerawebsocket.camera.CameraHelper;
import com.lisn.h265withcamerawebsocket.codec.DecodeH265;
import com.lisn.h265withcamerawebsocket.codec.EncodeH265;
import com.lisn.h265withcamerawebsocket.socket.BaseWebSocket;
import com.lisn.h265withcamerawebsocket.socket.LiveWebSocketClient;
import com.lisn.h265withcamerawebsocket.socket.LiveWebSocketServer;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 4:54 PM
 * @desc :
 */
class LiveManager {
    private SurfaceHolder localHolder;
    private SurfaceHolder remoteHolder;
    private CameraHelper cameraHelper;
    private int previewWidth;
    private int previewHeight;
    private Surface remoteSurface;
    private BaseWebSocket webSocket;

    private EncodeH265 encodeH265 = new EncodeH265();
    private DecodeH265 decodeH265 = new DecodeH265();

    public LiveManager(SurfaceHolder localHolder, SurfaceHolder remoteHolder) {

        this.localHolder = localHolder;
        this.remoteHolder = remoteHolder;
    }

    public void init(int width, int height) {

        cameraHelper = new CameraHelper(localHolder, width, height);
        localHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // 1.SurfaceView创建好后，开启摄像头预览
                cameraHelper.startPreview();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraHelper.stopPreview();
            }
        });
        cameraHelper.setPreviewListener(new CameraHelper.IPreviewListener() {

            @Override
            public void onPreviewSize(int width, int height) {
                // 2.预览成功
                previewWidth = width;
                previewHeight = height;
            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // 3.编码视频
                if (webSocket != null) {
                    encodeH265.encodeFrame(data);
                }
            }
        });

        remoteHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                //  SurfaceView创建好了
                remoteSurface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    public void start(boolean isServer) {
        if (previewHeight == 0 || previewWidth == 0) {
            Log.e("H265", "previewHeight==0 || previewWidth==0");
            return;
        }
        if (remoteSurface == null) {
            Log.e("H265", "remoteSurface==null");
            return;
        }
        Log.i("H265", "previewWidth=" + previewWidth + ",previewHeight=" + previewHeight);
        // 创建webSocket
        if (isServer) {
            webSocket = new LiveWebSocketServer();
        } else {
            webSocket = new LiveWebSocketClient();
        }

        webSocket.setH265ReceiveListener(new BaseWebSocket.IH265ReceiveListener() {

            @Override
            public void onReceive(byte[] data) {
                decodeH265.decode(data);
            }
        });
        webSocket.start();

        // 编码器
        encodeH265.initEncoder(previewWidth, previewHeight);
        encodeH265.setH265DecodeListener(new EncodeH265.IH265DecodeListener() {
            @Override
            public void onDecode(byte[] data) {
                webSocket.sendData(data);
            }
        });
        // 解码器
        decodeH265.initDecoder(remoteSurface, previewWidth, previewHeight);
    }

    public void stop() {
        if (webSocket != null) {
            webSocket.release();
            webSocket = null;
        }
        encodeH265.releaseEncoder();
        decodeH265.releaseDecoder();
    }
}
