package com.lisn.h265withcamerawebsocket.util;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/8 2:00 PM
 * @desc :
 */
public class YUVUtil {

    public static byte[] nv21toNv12(byte[] nv21) {
        int length = nv21.length;
        byte[] nv12 = new byte[length];
        int y_len = length * 2 / 3;
        // Y
        System.arraycopy(nv21, 0, nv12, 0, y_len);
        int i = y_len;
        // nv12和nv21是奇偶交替
        while (i < length - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public static void dataTo90(byte[] data, byte[] output, int width, int height) {
        int y_len = width * height;
        // uv数据高为y数据高的一半
        int uvHeight = height >> 1;
        int k = 0;

        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i > 0; i--) {
                output[k++] = data[width * i + j];
            }
        }

        // uv
        int j = 0;
        while (j < width) {
            for (int i = uvHeight - 1; i > 0; i--) {
                output[k++] = data[y_len + width * i + j];
                output[k++] = data[y_len + width * i + j + 1];
            }
            j += 2;
        }

    }

}
