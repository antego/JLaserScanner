package org.antego.dev;

import java.util.Arrays;

/**
 * Created by anton on 10.03.2015.
 */
public class FormulaSolver {
    private boolean valuesSet;
    private double h;
    private double aMan;
    private double bMan;
    private double tanAlpha;
    private double shaftX;
    private double shaftY;


    public void setVars(double th, double fi, double alfa, double h, double shaftX, double shaftY) {
        this.h = h;
        aMan = -1 * Math.tan(Math.toRadians(th)) / h;
        bMan = Math.tan(Math.toRadians(fi)) / h;
        tanAlpha = Math.tan(Math.toRadians(alfa));
        this.shaftX = shaftX;
        this.shaftY = shaftY;
        valuesSet = true;
    }

    public synchronized double[][] getCoordinates(double[][] frameCoords, double angle, int frameWidth, int frameHeight) {
        if (valuesSet) {
            double[][] profileCoords = Arrays.stream(frameCoords).map(rc -> {
                double[] xyz = new double[3];
                xyz[0] = findDistance(rc[1], frameWidth);
                xyz[1] = (frameWidth / 2 - rc[1]) / (frameWidth / 2) * xyz[0] * aMan * -1 * h;
                xyz[2] = ((frameHeight / 2) - rc[0]) / (frameHeight / 2) * xyz[0] * tanAlpha;
                return xyz;
            }).toArray(double[][]::new);

            return turnProfile(profileCoords, Math.toRadians(angle));
        } else {
            throw new RuntimeException("Position parameters not set");
        }
    }

    private double[][] turnProfile(double[][] coords, double angle) {
        double[][] newCoords = new double[coords.length][coords[0].length];
        for (int i = 0; i < coords.length; i++) {
            newCoords[i][0] = (coords[i][0] - shaftX) * Math.cos(angle) - (coords[i][1] - shaftY) * Math.sin(angle);
            newCoords[i][1] = (coords[i][1] - shaftY) * Math.cos(angle) + (coords[i][0] - shaftX) * Math.sin(angle);
            newCoords[i][2] = coords[i][2];
        }
        return newCoords;
    }

    private synchronized double findDistance(double x, int frameWidth) {
        return 1 / (bMan + aMan * (frameWidth / 2 - x) / (frameWidth / 2));
    }
}
