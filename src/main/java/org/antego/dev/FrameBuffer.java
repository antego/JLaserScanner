package org.antego.dev;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class FrameBuffer {
    private volatile Mat frame;
    private final Object frameLock = new Object();
    private VideoCapture camera;
    private final Queue<Future<Mat>> cloneTaskQueue = new LinkedList<>();

    private Thread cloningThread;
    private Thread assigningThread;

    public static int frameWidth;
    public static int frameHeight;

    public FrameBuffer(int camId, int frameWidth, int frameHeight) throws CameraNotOpenedException{
        camera = new VideoCapture(camId);
        long start_time = System.currentTimeMillis();
        while (!camera.isOpened()) {
            if(System.currentTimeMillis() - start_time > 1000)
                throw new CameraNotOpenedException("Can't open camera " + camId + ", check that camera connected and try another id.");
        }
        camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, frameWidth);
        camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, frameHeight);
        FrameBuffer.frameWidth = frameWidth;
        FrameBuffer.frameHeight = frameHeight;
        runThreads();
    }

    public void runThreads() {
        cloningThread = new Thread(() -> {
            ExecutorService cloneTasksExecutor = Executors.newFixedThreadPool(1);
            Mat tmpFrame = new Mat();
            while (!Thread.currentThread().isInterrupted()) {
                if (!camera.read(tmpFrame)) {
                    break;
                }
                CloneTask cloneTask = new CloneTask(tmpFrame);
                synchronized (cloneTaskQueue) {
                    cloneTaskQueue.add(cloneTasksExecutor.submit(cloneTask));
                    cloneTaskQueue.notify();
                }
            }
            cloneTasksExecutor.shutdown();
            camera.release();
        });
        assigningThread = new Thread(() -> {
            Future<Mat> futureMat;
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (cloneTaskQueue) {
                    try {
                        futureMat = cloneTaskQueue.poll();
                        if(futureMat == null) {
                            cloneTaskQueue.wait();
                        } else {
                            synchronized (frameLock) {
                                frame = futureMat.get();
                                frameLock.notify();
                            }
                        }
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        cloningThread.start();
        assigningThread.start();
    }

    public void stop() {
        cloningThread.interrupt();
        assigningThread.interrupt();
    }

    public Mat getFrame() {
        synchronized (frameLock) {
            while (frame == null) {
                try {
                    frameLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return frame;
        }
    }

    public static class CameraNotOpenedException extends Exception {
        public CameraNotOpenedException(String m) {
            super(m);
        }
    }

    private class CloneTask implements Callable<Mat> {
        private Mat frameToClone;

        public CloneTask(Mat frameToClone) {
            this.frameToClone = frameToClone;
        }

        @Override
        public Mat call() throws Exception {
            return frameToClone.clone();
        }
    }
}
