package com.lisn.h265withcamerawebsocket.camera;


import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:01 PM
 * @desc :
 */
public class CameraHelper implements Camera.PreviewCallback {
    private static final String TAG = CameraHelper.class.getSimpleName();
    private SurfaceHolder holder;
    private int width;
    private int height;
    private IPreviewListener previewListener;
    private Camera camera;
    private byte[] buffer;

    public CameraHelper(SurfaceHolder localHolder, int width, int height) {
        this.holder = localHolder;
        this.width = width;
        this.height = height;
    }

    public void startPreview() {
        try {
            // 临时用后置摄像头，重点是编解码和数据的传输
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters parameters = camera.getParameters();
            // 摄像头默认NV21
            Log.e(TAG, "startPreview: PreviewFormat = " + parameters.getPreviewFormat());
            Camera.Size size = getPreviewSize(parameters);
            width = size.width;
            height = size.height;
            parameters.setPreviewSize(width, height);
            Log.d(TAG, "设置预览分辨率 width:" + width + "height:" + height);

            camera.setParameters(parameters);

            camera.setPreviewDisplay(holder);
            // 由于硬件安装是横着的，如果是后置摄像头&&正常竖屏的情况下需要旋转90度
            // 只是预览旋转了，数据没有旋转
            camera.setDisplayOrientation(90);

            // 让摄像头回调一帧的数据大小
            buffer = new byte[width * height * 3 / 2];
            // onPreviewFrame回调的数据大小就是buffer.length
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
            if (previewListener != null) {
                previewListener.onPreviewSize(width, height);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "支持 " + size.width + "X" + size.height);
        supportedPreviewSizes.remove(0);
        int m = Math.abs(size.width * size.height - width * height);

        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        while (iterator.hasNext()) {
            Camera.Size next = iterator.next();
            Log.d(TAG, "支持 " + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - width * height);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        return size;
    }

    public void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            Log.d(TAG, "camera release ok");
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (previewListener != null) {
            previewListener.onPreviewFrame(data, camera);
        }
        camera.addCallbackBuffer(data);
    }

    public void setPreviewListener(IPreviewListener previewListener) {
        this.previewListener = previewListener;
    }

    public interface IPreviewListener {
        void onPreviewSize(int width, int height);

        void onPreviewFrame(byte[] data, Camera camera);
    }
}
