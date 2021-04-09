package com.lisn.h265withcamerawebsocket.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:34 PM
 * @desc :
 */
public class DecodeH265 {
    private static final String TAG = DecodeH265.class.getSimpleName();
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    public void initDecoder(Surface surface, int width, int height) {
        if (mediaCodec != null) {
            return;
        }
        try {
            // H265解码器
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800_000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            // 渲染到surface上
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decode(byte[] data) {
        int index = mediaCodec.dequeueInputBuffer(10_000);
        if (index >= 0) {
            // 送入数据
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(index);
            byteBuffer.clear();
            byteBuffer.put(data);
            mediaCodec.queueInputBuffer(
                    index, 0, data.length,
                    System.currentTimeMillis(), 0
            );
        }
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 10_000);
        while (outputBufferIndex >= 0) {
            // true渲染到surface上
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 0);
        }
    }

    public void releaseDecoder() {
        try {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
                Log.d(TAG, "releaseDecoder ok");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
