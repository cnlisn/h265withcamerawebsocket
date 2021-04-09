package com.lisn.h265withcamerawebsocket.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.lisn.h265withcamerawebsocket.util.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:34 PM
 * @desc :
 */
public class EncodeH265 {

    private static final String TAG = EncodeH265.class.getSimpleName();

    private static final int NAL_I = 19;
    private static final int NAL_VPS = 32;
    private static final int FPS = 15;

    private IH265DecodeListener h265DecodeListener;
    private int previewWidth;
    private int previewHeight;
    private MediaCodec mediaCodec;
    private byte[] yuv;
    private byte[] nv12;
    private long frameIndex = 0;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private byte[] vps_sps_pps_buf;

    public void initEncoder(int width, int height) {
        if (mediaCodec != null) {
            return;
        }
        this.previewWidth = width;
        this.previewHeight = height;
        try {
            // H265编码器 video/hevc
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);

            // 宽高对调的原因是：后置摄像头旋转了90度，yuv数据也旋转了90度
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800_000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mediaFormat.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            );
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            int bufferLength = width * height * 3 / 2;
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encodeFrame(byte[] data) {
        // nv21 格式转换成nv12
        nv12 = YUVUtil.nv21toNv12(data);

        // 数据旋转90度
        YUVUtil.dataTo90(nv12, yuv, previewWidth, previewHeight);

        // 开始编码
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(10_1000);
        if (inputBufferIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            byteBuffer.clear();
            byteBuffer.put(yuv);
            // PTS
            // 1。 +132的目的是解码端初始化播放器需要时间，防止播放器首帧没有播放的问题，不一定是132us
            // 2。 frameIndex初始值=1，不加132，也可以。
            long presentationTimeUs = 132 + frameIndex * 1000_000 / FPS;
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }

        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 10_000);
        while (outputBufferIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(byteBuffer);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 0);
        }

    }

    private void dealFrame(ByteBuffer byteBuffer) {
        // H265的nalu的分割符的下一个字节的类型
        int offset = 4;
        if (byteBuffer.get(2) == 0x1) {
            offset = 3;
        }
        // VPS,SPS,PPS...  H265的nalu头是2个字节，中间的6位bit是nalu类型

        // 0x7E的二进制的后8位是 0111  1110
        int naluType = (byteBuffer.get(offset) & 0x7E) >> 1;
        Log.d(TAG, "naluType=" + naluType);
        // 保存下VPS,SPS,PPS的数据
        if (NAL_VPS == naluType) {
            vps_sps_pps_buf = new byte[info.size];
            byteBuffer.get(vps_sps_pps_buf);
            Log.d(TAG, "vps_sps_pps_buf size =" + vps_sps_pps_buf.length);
        } else if (NAL_I == naluType) {
            // 因为是网络传输，所以在每个i帧之前先发送VPS,SPS,PPS
            byte[] bytes = new byte[info.size];
            byteBuffer.get(bytes);
            byte[] newBuf = new byte[info.size + vps_sps_pps_buf.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);

            // 发送
            Log.d(TAG, "send I帧:" + newBuf.length);
            h265DecodeListener.onDecode(newBuf);
        } else {
            // 其它bp帧数据
            byte[] bytes = new byte[info.size];
            byteBuffer.get(bytes);

            // 发送
            Log.d(TAG, "send P/B帧:" + bytes.length);
            h265DecodeListener.onDecode(bytes);
        }
    }

    public void releaseEncoder() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        Log.d(TAG, "releaseEncoder ok");
    }

    public void setH265DecodeListener(IH265DecodeListener h265DecodeListener) {
        this.h265DecodeListener = h265DecodeListener;
    }

    public interface IH265DecodeListener {
        void onDecode(byte[] data);
    }
}
