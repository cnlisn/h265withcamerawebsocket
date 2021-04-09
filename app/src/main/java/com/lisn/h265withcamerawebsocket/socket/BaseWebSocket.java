package com.lisn.h265withcamerawebsocket.socket;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2021/4/7 5:20 PM
 * @desc :
 */
public abstract class BaseWebSocket {

    public abstract void sendData(byte[] bytes);

    public abstract void start();

    public abstract void release();

    IH265ReceiveListener h265ReceiveListener = null;


    public void setH265ReceiveListener(IH265ReceiveListener h265ReceiveListener) {
        this.h265ReceiveListener = h265ReceiveListener;
    }

    public interface IH265ReceiveListener {
        void onReceive(byte[] data);
    }
}
