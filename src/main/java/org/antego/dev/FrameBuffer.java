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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FrameBuffer implements FrameProducer {
    public static int frameWidth;
    public static int frameHeight;
    private final Object frameLock = new Object();
    private final VideoCapture camera;
    private final Queue<Future<Mat>> cloneTaskQueue = new LinkedList<>();
    private volatile Mat frame;
    private Thread cloningThread;
    private Thread assigningThread;

    public FrameBuffer(int camId, int frameWidth, int frameHeight) throws CameraNotOpenedException {
        camera = new VideoCapture(camId);
        long start_time = System.currentTimeMillis();
        while (!camera.isOpened()) {
            if (System.currentTimeMillis() - start_time > 1000)
                throw new CameraNotOpenedException("Can't open camera " + camId + ", check that camera connected and try another id.");
        }
        camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, frameWidth);
        camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, frameHeight);
        FrameBuffer.frameWidth = frameWidth;
        FrameBuffer.frameHeight = frameHeight;
        cloningThread = new Thread(() -> {
            ExecutorService cloneTasksExecutor = Executors.newFixedThreadPool(3);
            Mat tmpFrame = new Mat();
            while (!Thread.currentThread().isInterrupted()) {
                if (!camera.read(tmpFrame) || tmpFrame.empty()) {
                    continue;
                }
                //CloneTask cloneTask = new CloneTask(tmpFrame);
                synchronized (cloneTaskQueue) {
                    cloneTaskQueue.add(new Future<Mat>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            return false;
                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }

                        @Override
                        public boolean isDone() {
                            return false;
                        }

                        @Override
                        public Mat get() throws InterruptedException, ExecutionException {
                            return tmpFrame;
                        }

                        @Override
                        public Mat get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                            return null;
                        }
                    });
                    cloneTaskQueue.notify();
                }
            }
            cloneTasksExecutor.shutdown();
            camera.release();
        });

        assigningThread = new Thread(() -> {
            Future<Mat> futureMat;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (cloneTaskQueue) {
                        futureMat = cloneTaskQueue.poll();
                        while (futureMat == null) {
                            cloneTaskQueue.wait();
                            futureMat = cloneTaskQueue.poll();
                        }
                        synchronized (frameLock) {
                            try {
                                frame = futureMat.get();
                                frameLock.notifyAll();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
            }
        });
        cloningThread.setName("cloning thread");
        assigningThread.setName("assigning thread");
        cloningThread.start();
        assigningThread.start();
    }

    public void stop() {
        cloningThread.interrupt();
        assigningThread.interrupt();
    }

    public Mat getFrame() {
        synchronized (frameLock) {
            try {
                while (frame == null) {
                    frameLock.wait();
                }
            } catch (InterruptedException e) {
            }
            Mat tmpFrame = frame;
            frame = null;
            return tmpFrame;
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
