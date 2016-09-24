package com.github.antego.laserscanner;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class ImageProcessor {
    private static final int WHITE_RGB = new Color(255, 255, 255).getRGB();

    public static final double DEFAULT_HUE1_MIN = 170;
    public static final double DEFAULT_HUE1_MAX = 179;
    public static final double DEFAULT_HUE2_MIN = 0;
    public static final double DEFAULT_HUE2_MAX = 10;
    public static final double DEFAULT_SAT_MIN = 200;
    public static final double DEFAULT_SAT_MAX = 255;
    public static final double DEFAULT_VAL_MIN = 200;
    public static final double DEFAULT_VAL_MAX = 255;

    private double minHue = DEFAULT_HUE1_MIN;
    private double maxHue = DEFAULT_HUE1_MAX;
    private double minSat = DEFAULT_SAT_MIN;
    private double maxSat = DEFAULT_SAT_MAX;
    private double minVal = DEFAULT_VAL_MIN;
    private double maxVal = DEFAULT_VAL_MAX;

    private boolean showRawImg;

    public synchronized void setThresholds(double hue1Min,
                                           double hue1Max,
                                           double satMin,
                                           double satMax,
                                           double valMin,
                                           double valMax) {
        this.minHue = hue1Min;
        this.maxHue = hue1Max;
        this.minSat = satMin;
        this.maxSat = satMax;
        this.minVal = valMin;
        this.maxVal = valMax;
    }

    public synchronized void setShowRawImg(boolean showRawImg) {
        this.showRawImg = showRawImg;
    }

    public synchronized double[][] findDots(BufferedImage frame) {
        ImagePlus image = new ImagePlus(null, frame);
        ImageConverter converter = new ImageConverter(image);
        converter.convertToHSB();
        ImagePlus filteredImage = filter(image);

        List<Double[]> lineCenters = new ArrayList<>();
        for (int j = 0; j < image.getHeight(); j++) {
            int count = 0;
            for (int k = 0; k < image.getWidth(); k++) {
                if (filteredImage.getPixel(k, j)[0] != 0) count++;
            }
            if (count > 0) {
                double[] coords = new double[count];
                count = 0;
                for (int k = 0; k < image.getWidth(); k++) {
                    if (filteredImage.getPixel(k, j)[0] != 0) {
                        coords[count] = k;
                        count++;
                    }
                }
                if (coords.length % 2 == 0) {
                    lineCenters.add(new Double[]{(double) j,
                            (coords[coords.length / 2] + coords[coords.length / 2 - 1]) / 2});
                } else {
                    lineCenters.add(new Double[]{(double) j,
                            coords[coords.length / 2]});
                }
                //show chosen dots as black
                frame.setRGB(j, lineCenters.get(lineCenters.size() - 1)[1].intValue(), WHITE_RGB);
            }
        }
        if (showRawImg) {
            frame.setData(filteredImage.getBufferedImage().getData());
        }
        double[][] lineCentersArray = new double[lineCenters.size()][2];
        for (int i = 0; i < lineCenters.size(); i++) {
            lineCentersArray[i][0] = lineCenters.get(i)[0];
            lineCentersArray[i][1] = lineCenters.get(i)[1];
        }
        return lineCentersArray;
    }

    private ImagePlus filter(ImagePlus frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int numPixels = width*height;

        byte[] hSource = new byte[numPixels];
        byte[] sSource = new byte[numPixels];
        byte[] bSource = new byte[numPixels];
        ColorProcessor cp = frame.getProcessor().convertToColorProcessor();
        cp.getHSB(hSource,sSource,bSource);

        ij.process.ImageProcessor maskIp = new ByteProcessor(width, height);
        byte[] fillMask = (byte[])maskIp.getPixels();
        byte fill = (byte)255;
        byte keep = (byte)0;
        for (int j = 0; j < numPixels; j++){
            int hue = hSource[j]&0xff;
            int sat = sSource[j]&0xff;
            int bri = bSource[j]&0xff;

            if ((minHue < maxHue)&&((hue < minHue)||(hue > maxHue))) {
                fillMask[j] = keep;
            } else if ((minHue > maxHue)&&((hue < minHue)&&(hue > maxHue))) {
                fillMask[j] = keep;
            } else if (((sat < minSat)||(sat > maxSat)) || ((bri < minVal)||(bri > maxVal))) {
                fillMask[j] = keep;
            } else {
                fillMask[j] = fill;
            }
        }

        return new ImagePlus(null, maskIp);
    }
}
