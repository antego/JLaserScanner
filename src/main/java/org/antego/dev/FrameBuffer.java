package org.antego.dev;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;


public class FrameBuffer implements Runnable {
    public final int frameWidth;
    public final int frameHeight;

    private volatile Mat frame = new Mat();
    private VideoCapture camera;
    private Thread thread;

    public FrameBuffer(int camId, int frameWidth, int frameHeight) throws CameraNotOpenedException {
        camera = new VideoCapture(camId);
        long start_time = System.currentTimeMillis();
        while (!camera.isOpened()) {
            if (System.currentTimeMillis() - start_time > 1000)
                throw new CameraNotOpenedException("Can'thread open camera " + camId + ", check that camera connected and try another id.");
        }
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, this.frameWidth);
        camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, this.frameHeight);
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Mat newFrame = new Mat();
            if (!camera.read(newFrame)) {
                break;
            }
            frame = newFrame;
        }
        camera.release();
    }

    public void stop() {
        thread.interrupt();
    }

    public Mat getFrame() {
        return frame;
    }

    public static class CameraNotOpenedException extends Exception {
        public CameraNotOpenedException(String m) {
            super(m);
        }
    }
}
