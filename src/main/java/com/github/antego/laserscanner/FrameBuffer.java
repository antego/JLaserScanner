package com.github.antego.laserscanner;

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;

//todo error handling
public class FrameBuffer {
    private Webcam webcam;

    public FrameBuffer() {
        webcam = Webcam.getDefault();
        if (!webcam.open()) {
            throw new RuntimeException("Camera nor opened");
        }
    }

    public void stop() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    public BufferedImage getFrame() {
        return webcam.getImage();
    }

    public static class CameraNotOpenedException extends Exception {
        public CameraNotOpenedException(String m) {
            super(m);
        }
    }
}
