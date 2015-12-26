package org.antego.dev;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by anton on 26.12.15.
 */
class MatToImageConverter {
    final Queue<Future<Image>> imageQueue = new LinkedList<>();
    final Thread imageDisplayer = new Thread(new ImageDisplayer());
    final Thread imageFetcher = new Thread(new ImageFetcher());
    final FrameProducer frameProducer;
    final ImageView imageView;
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    MatToImageConverter(FrameProducer frameProducer, ImageView imageView) {
        this.frameProducer = frameProducer;
        this.imageView = imageView;
        imageFetcher.setName("Fetcher thread");
        imageDisplayer.setName("Displayer thread");
        imageFetcher.start();
        imageDisplayer.start();
    }

    void stop() {
        imageDisplayer.interrupt();
        imageFetcher.interrupt();
        executorService.shutdown();
        frameProducer.stop();
    }

    private class ImageDisplayer implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (imageQueue) {
                        while (imageQueue.isEmpty()) {
                            imageQueue.wait();
                        }
                        Image imageToDisplay = imageQueue.poll().get();
                        Platform.runLater(() -> imageView.setImage(imageToDisplay));
                    }
                }
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private class ImageFetcher implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Mat frame = frameProducer.getFrame();
                if (frame == null || frame.empty()) {
                    continue;
                }
                synchronized (imageQueue) {
                    imageQueue.add(executorService.submit(() -> {
                        MatOfByte buffer = new MatOfByte();
                        Highgui.imencode(".bmp", frame, buffer);
                        frame.release();
                        return new Image(new ByteArrayInputStream(buffer.toArray()));
                    }));
                    imageQueue.notify();
                }
            }
        }
    }

}
