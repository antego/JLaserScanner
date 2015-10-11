package sample;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by anton on 09.03.2015.
 */
public class ImageProcessor {
    public static final int THRESHOLD = 250;
    public static double[] findDots(Mat frame) {
        //Count of red dots
        int count = 0;
        double[] median = new double[FrameBuffer.frameHeight];
        for (int j = 0; j < FrameBuffer.frameHeight; j++) {
            for (int k = 0; k < FrameBuffer.frameWidth; k++) {
                if (frame.get(j, k)[2] > THRESHOLD) count++;
            }
            if (count > 0) {
                double[] coords = new double[count];
                count = 0;
                for (int k = 0; k < FrameBuffer.frameHeight; k++) {
                    if (frame.get(j, k)[2] > THRESHOLD) {
                        //массив координат х ов больше 230
                        coords[count] = k;
//                        coords[count][1] = frame.get(j, k)[2];
                        count++;
                    }
                }
                //Center coordinate of dot
                if (coords.length % 2 == 0) {
                    median[j] = (coords[coords.length / 2] + coords[coords.length / 2 - 1]) / 2;
                } else {
                    median[j] = coords[coords.length / 2 - 1];
                }
                //make choosen dot red
                frame.put(j, (int) median[j], new double[]{0, 0, 255});
            } else median[j] = -1;
            count = 0;
        }
        return median;
    }
}
