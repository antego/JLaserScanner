package sample;

import org.opencv.core.Mat;

/**
 * Created by anton on 16.03.2015.
 */
public interface DotsFinder {
    public  double[] findDots(Mat frame);
}
