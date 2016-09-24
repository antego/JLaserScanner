package com.github.antego.laserscanner;

import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ImageProcessorTest {
    ImageProcessor imp = new ImageProcessor();

    @Before
    public void setupImageProcessor() {
        imp.setThresholds(0, 255, 0, 255, 0, 255);
    }

    @Test
    public void findDotsTest() throws IOException {
        BufferedImage img = ImageIO.read(new File("src/test/resources/test-img.png"));
        double[][] dots = imp.findDots(img);
        assertEquals(img.getHeight(), dots.length);
        assertEquals(img.getWidth() / 2, dots[0][1], .001);
    }
}
