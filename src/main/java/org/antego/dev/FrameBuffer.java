package org.antego.dev;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;


public class FrameBuffer {
    public static int frameWidth = 640;
    public static int frameHeight = 480;

    volatile Mat frame = new Mat();
    VideoCapture camera;
    Thread t ;

    public FrameBuffer(int camId, int frameWidth, int frameHeight) throws CameraNotOpenedException{
        camera = new VideoCapture(camId);
        long start_time = System.currentTimeMillis();
        //maybe faster check with .isOpen()
        while (!camera.isOpened()) {
            if(System.currentTimeMillis() - start_time > 1000)
                throw new CameraNotOpenedException("Can't open camera " + camId + ", check that camera connected and try another id.");
        }
        FrameBuffer.frameWidth = frameWidth;
        FrameBuffer.frameHeight = frameHeight;
        camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, FrameBuffer.frameWidth);
        camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, FrameBuffer.frameHeight);
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
            while (!Thread.interrupted()) {
                //maybe faster check with .grab()
                if (!camera.read(lastFrame)) {
                    break;
                }
            }
            camera.release();
        }
    }

    public static class CameraNotOpenedException extends Exception {
        /*
        If a constructor does not explicitly invoke a superclass constructor,
        the Java compiler automatically inserts a call to the no-argument constructor of the superclass.*/
        public CameraNotOpenedException(){
        }

        public CameraNotOpenedException(String m) {
            super(m);
        }

        public CameraNotOpenedException(Throwable t) {
            super(t);
        }
        public CameraNotOpenedException(String m, Throwable t) {
            super(m, t);
        }
    }
}
