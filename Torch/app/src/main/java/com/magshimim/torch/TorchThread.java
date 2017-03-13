package com.magshimim.torch;
import android.graphics.Bitmap;
import java.lang.Object;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.io.*;
import java.net.DatagramPacket;
import android.util.Log;
import java.net.DatagramSocket;


public class TorchThread extends Thread {
    private static final String TAG = "TorchThread";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final Queue<Bitmap> framesToSend;
    private DatagramSocket socket;
    private boolean sending;

    public TorchThread(String name, final Queue<Bitmap> framesToSend, DatagramSocket socket){
        super(name);
        if(DEBUG) Log.d(TAG, "TorchThread");
        this.framesToSend = framesToSend;
        this.socket=socket;
        sending = false;
    }

    @Override
    public void run() {
        if(DEBUG) Log.d(TAG, "run");
        sending = true;
        Bitmap frame;

        while (sending) {
            if(DEBUG) Log.d(TAG, "waiting for frame");
            synchronized (framesToSend) {
                try {
                    framesToSend.wait(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error", e);
                    continue;
                }
                if (!framesToSend.isEmpty()) {
                    frame = framesToSend.poll();
                    if(DEBUG) Log.d(TAG, "got frame");
                }
                else {
                    Log.w(TAG, "queue is empty");
                    continue;
                }
            }

            ByteBuffer buffer = ByteBuffer.allocate(frame.getByteCount());
            frame.copyPixelsToBuffer(buffer);
            if(DEBUG) Log.d(TAG, "copied frame to buffer");
            byte[] arrayToSend = buffer.array();
            DatagramPacket packet = new DatagramPacket(arrayToSend, arrayToSend.length);
            if(DEBUG) Log.d(TAG, "Created datagram packet, length = " + packet.getLength());
            try {
                socket.send(packet);
                if(DEBUG) Log.d(TAG, "packet is sent");
            } catch (IOException e) {
                Log.e("NetworkManager,Frame", e.getMessage());
            }
        }
    }

    public void stopSending()
    {
        if(DEBUG) Log.d(TAG, "stopSending");
        sending = false;
    }
}
