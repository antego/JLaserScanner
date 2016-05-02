package org.antego.dev;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class ImageProcessor {
    public static final double DEFAULT_HUE1_MIN = 170;
    public static final double DEFAULT_HUE1_MAX = 179;
    public static final double DEFAULT_HUE2_MIN = 0;
    public static final double DEFAULT_HUE2_MAX = 10;
    public static final double DEFAULT_SAT_MIN = 200;
    public static final double DEFAULT_SAT_MAX = 255;
    public static final double DEFAULT_VAL_MIN = 200;
    public static final double DEFAULT_VAL_MAX = 255;

    private double hue1Min = DEFAULT_HUE1_MIN;
    private double hue1Max = DEFAULT_HUE1_MAX;
    private double hue2Min = DEFAULT_HUE2_MIN;
    private double hue2Max = DEFAULT_HUE2_MAX;
    private double satMin = DEFAULT_SAT_MIN;
    private double satMax = DEFAULT_SAT_MAX;
    private double valMin = DEFAULT_VAL_MIN;
    private double valMax = DEFAULT_VAL_MAX;

    private boolean showRawImg;

    public synchronized void setThresholds(double hue1Min,
                                           double hue1Max,
                                           double hue2Min,
                                           double hue2Max,
                                           double satMin,
                                           double satMax,
                                           double valMin,
                                           double valMax) {
        this.hue1Min = hue1Min;
        this.hue1Max = hue1Max;
        this.hue2Min = hue2Min;
        this.hue2Max = hue2Max;
        this.satMin = satMin;
        this.satMax = satMax;
        this.valMin = valMin;
        this.valMax = valMax;
    }

    public synchronized void setShowRawImg(boolean showRawImg) {
        this.showRawImg = showRawImg;
    }

    public synchronized double[][] findDots(Mat frame) {
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
        Mat part1 = new Mat();
        Mat part2 = new Mat();
        Mat result = new Mat();
        Core.inRange(frame, new Scalar(hue1Min, satMin, valMin), new Scalar(hue1Max, satMax, valMax), part2);
        Core.inRange(frame, new Scalar(hue2Min, satMin, valMin), new Scalar(hue2Max, satMax, valMax), part1);
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_HSV2BGR);
        Core.add(part1, part2, result);

        List<Double[]> lineCenter = new ArrayList<>();
        for (int j = 0; j < frame.height(); j++) {
            int count = 0;
            for (int k = 0; k < frame.width(); k++) {
                if (result.get(j, k)[0] != 0) count++;
            }
            if (count > 0) {
                double[] coords = new double[count];
                count = 0;
                for (int k = 0; k < frame.width(); k++) {
                    if (result.get(j, k)[0] != 0) {
                        coords[count] = k;
                        count++;
                    }
                }
                if (coords.length % 2 == 0) {
                    lineCenter.add(new Double[]{(double) j, (coords[coords.length / 2] + coords[coords.length / 2 - 1]) / 2});
                } else {
                    lineCenter.add(new Double[]{(double) j, coords[coords.length / 2]});
                }
                //show chosen dots as black
                frame.put(j, lineCenter.get(lineCenter.size() - 1)[1].intValue(), 0, 0, 0);
            }
        }
        if (showRawImg) {
            result.copyTo(frame);
        }
        double[][] median = new double[lineCenter.size()][2];
        for (int i = 0; i < lineCenter.size(); i++) {
            median[i][0] = lineCenter.get(i)[0];
            median[i][1] = lineCenter.get(i)[1];
        }
        return median;
    }
}
