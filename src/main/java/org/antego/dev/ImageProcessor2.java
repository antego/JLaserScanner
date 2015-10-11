package org.antego.dev;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by anton on 16.03.2015.
 */
public class ImageProcessor2 {
    public static final int THRESHOLD = 250;
    double hue2Min = 0;
    double hue2Max = 10;
    double hue1Min = 170;
    double hue1Max = 179;
    double satMin = 200;
    double satMax = 255;
    double valMin = 200;
    double valMax = 255;

    boolean showRawImg = false;

    public synchronized void setThresholds(double hue1Min, double hue1Max, double hue2Min, double hue2Max, double satMin, double satMax, double valMin, double valMax) {
        this.hue1Min = hue1Min;
        this.hue1Max = hue1Max;
        this.hue2Min = hue2Min;
        this.hue2Max = hue2Max;
        this.satMin = satMin;
        this.satMax = satMax;
        this.valMin = valMin;
        this.valMax = valMax;
    }

    public synchronized double[] getThresholds() {
        return new double[]{hue1Min,hue1Max,hue2Min,hue2Max,satMin,satMax,valMin,valMax};
    }

    public synchronized void setShowRawImg(boolean showRawImg) {
        this.showRawImg = showRawImg;
    }

    public synchronized double[] findDots(Mat frame, Mat mask) {
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
        double[] minValues = new double[]{12, 23, 23};
        double[] maxValues = new double[]{12, 23, 23};

        Mat part1 = new Mat();
        Mat part2 = new Mat();
        Mat result = new Mat();
        Core.inRange(frame, new Scalar(hue1Min, satMin, valMin), new Scalar(hue1Max, satMax, valMax), part2);
        Core.inRange(frame, new Scalar(hue2Min, satMin, valMin), new Scalar(hue2Max, satMax, valMax), part1);
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_HSV2BGR);
        Core.add(part1, part2, result);
        if(mask == null)
            mask = new Mat(result.size(), CvType.CV_8UC1, new Scalar(255));
        Core.bitwise_and(result,mask,result);

        int count = 0;
        double[] median = new double[FrameBuffer.frameHeight];
        for (int j = 0; j < FrameBuffer.frameHeight; j++) {
            for (int k = 0; k < FrameBuffer.frameWidth; k++) {
                if (result.get(j, k)[0] > THRESHOLD) count++;
            }
            if (count > 0) {
                double[] coords = new double[count];
                count = 0;
                for (int k = 0; k < FrameBuffer.frameWidth; k++) {
                    if (result.get(j, k)[0] > THRESHOLD) {
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
                    median[j] = coords[coords.length / 2];
                }
                //make choosen dot red

                frame.put(j, (int) median[j], new double[]{0, 0, 0});

            } else median[j] = -1;
            count = 0;

        }
        if (showRawImg) result.copyTo(frame);
        return median;
    }
}
