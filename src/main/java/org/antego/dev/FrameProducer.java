package org.antego.dev;

import org.opencv.core.Mat;

/**
 * Created by anton on 26.12.15.
 */
public interface FrameProducer {
    Mat getFrame();

    void stop();
}
