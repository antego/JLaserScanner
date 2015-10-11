package org.antego.dev;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by anton on 10.03.2015.
 */
public class FormulaSolver {
    boolean manual_mode;
    boolean valuesSetted = false;
    //    double tanTheta;
//    double tanFi;
    double h;
    double a_man;
    double b_man;
    double a_auto;
    double b_auto;
    double tan_alfa;
    double shaft_x;
    double shaft_y;
    ArrayList<Measure> measures;


    public FormulaSolver(ArrayList<Measure> data) {
        this.measures = data;
    }


    public void setMode(boolean manual_mode) {
        this.manual_mode = manual_mode;
    }

    public synchronized double getDistance(double[] coords) {
        if (coords != null)
            return distance(coords[0]);
        else return 0;
    }

//    public double getDistance(double[] coords, double fi, double th, double h) {
//        this.setVars(th, fi, h);
//        if (coords != null)
//            return distance(coords[0]);
//        else return 0;
//    }

    public void setVars(double th, double fi, double alfa, double h, double shaft_x, double shaft_y) {
        manual_mode = true;
        this.h = h;
        a_man = -1 * Math.tan(Math.toRadians(th)) / h;
        b_man = Math.tan(Math.toRadians(fi)) / h;
        tan_alfa = Math.tan(Math.toRadians(alfa));
        this.shaft_x = shaft_x;
        this.shaft_y = shaft_y;
        valuesSetted = true;
    }

    public synchronized double[][] getCoordinates(double[] frameCoords, double angle) {
        if (valuesSetted) {
            int count = 0;
            for (double x : frameCoords) if (x != -1) count++;
            double[][] profileCoords = new double[count][3];
            int j = 0;
            for (int i = 0; i < frameCoords.length; i++) {
                if (frameCoords[i] != -1) {
                    profileCoords[j][0] = distance(frameCoords[i]);
                    profileCoords[j][1] = (FrameBuffer.frameWidth / 2 - frameCoords[i]) / (FrameBuffer.frameWidth / 2) * profileCoords[j][0] * a_man * -1 * h;
                    profileCoords[j][2] = ((FrameBuffer.frameHeight / 2) - (double) i) / (FrameBuffer.frameHeight / 2) * profileCoords[j][0] * tan_alfa;
                    j = j + 1;
                }

            }
            return turnProfile(profileCoords, Math.toRadians(angle));
        } else return null;
    }



    public double[][] turnProfile(double[][] coords, double angle) {
        if(coords != null) {
            double[][] newCoords = new double[coords.length][coords[0].length];
            for (int i = 0; i < coords.length;i++) {
                newCoords[i][0] = (coords[i][0] - shaft_x) * Math.cos(angle) - (coords[i][1] - shaft_y) * Math.sin(angle);
                newCoords[i][1] = (coords[i][1] - shaft_y) * Math.cos(angle) + (coords[i][0] - shaft_x) * Math.sin(angle);
                newCoords[i][2] = coords[i][2];
            }
            return newCoords;
        } else return null;
    }

    private synchronized double distance(double x) {
        if (manual_mode)
            return 1 / (b_man + a_man * (FrameBuffer.frameWidth / 2 - x) / (FrameBuffer.frameWidth / 2));
        else return 1 / (b_auto + a_auto * (FrameBuffer.frameWidth / 2 - x) / (FrameBuffer.frameWidth / 2));
    }

    public synchronized void addMeasure() {
//        measures.add(m);
        double[] coefs = Quadr.findCoefs(measures);
        System.out.println("After add a: " + coefs[0] + "b: " + coefs[1]);
        a_auto = coefs[0];
        b_auto = coefs[1];
    }

    public synchronized void deleteMeasure() {
//        Iterator<Measure> i = measures.iterator();
//        while (i.hasNext()) {
//            Measure meas = i.next();
//            if(meas.getX() == m.getX()) measures.remove(meas);
//        }
        double[] coefs = Quadr.findCoefs(measures);
        System.out.println("After delete a: " + coefs[0] + "b: " + coefs[1]);
        a_auto = coefs[0];
        b_auto = coefs[1];
    }
}
