package org.antego.dev;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

/**
 * Created by anton on 09.03.2015.
 */
public class FrameBuffer {
    public static int frameWidth = 640;
    public static int frameHeight = 480;

    volatile Mat frame = new Mat();
    VideoCapture camera;
    Thread t ;

    public FrameBuffer(int frameWidth, int frameHeight) throws Exception{
        camera = new VideoCapture(1);
        FrameBuffer.frameWidth = frameWidth;
        FrameBuffer.frameHeight = frameHeight;
        camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, FrameBuffer.frameWidth);
        camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, FrameBuffer.frameHeight);
        long start_time = System.currentTimeMillis();
        while (!camera.grab()) {
            if(System.currentTimeMillis() - start_time > 3000)
                throw new Exception("camera doesn't opened");
        }
        t = new Thread(new FrameFetcher(camera, frame));
        t.start();
    }

    public void stop() {
        t.interrupt();
    }

    public Mat getFrame() {
        return frame.clone();
    }

    private static class FrameFetcher implements Runnable {
        public volatile Mat lastFrame = new Mat();
        VideoCapture camera;
        public FrameFetcher(VideoCapture camera, Mat frame) {
            this.camera = camera;
            lastFrame = frame;
        }
        public void run() {
            while (!Thread.interrupted())
                if (!camera.read(lastFrame))
                    break;

            camera.release();
        }
    }
}
