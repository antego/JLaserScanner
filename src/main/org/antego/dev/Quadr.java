package sample;

import java.util.ArrayList;

public class Quadr {

    public static double[] findCoefs(ArrayList<Measure> measuresList) {
//        double[][] measures = new double[measuresList.size()][2];
//        for(int i = 0; i < measuresList.size();i++) {
//            measures[i][0] = measuresList.get(i).getX();
//            measures[i][1] = measuresList.get(i).getDistantion();
//        }
        //Обратные игрики
        double[] yy = new double[measuresList.size()];
        for(int i = 0; i < measuresList.size(); i++) yy[i] = 1 / measuresList.get(i).getDistantion();
        //New xs
        double[] xx = new double[measuresList.size()];
        for(int i = 0; i < measuresList.size(); i++) xx[i] = (FrameBuffer.frameWidth / 2 - measuresList.get(i).getX()) / FrameBuffer.frameWidth / 2;

        //Мх
        double mx = 0;
        for (double x : xx) mx += x / measuresList.size();
        //Му
        double my = 0;
        for (Double y : yy) my += y / measuresList.size();
        //Мху
        double mxy = 0;
        for (int i = 0; i < measuresList.size(); i++) mxy += xx[i] * yy[i] / measuresList.size();
        //Mx2
        double mx2 = 0;
        for (double x : xx) mx2 += x * x / measuresList.size();
        double a = (mxy - mx * my) / (mx2 - mx * mx);
        double b = my - mx * a;
        double dmin = 1 / (b - a);
        double dmax = 1 / (b + a);
//        double[] results = ;
//        results[0] = a;
//        results[1] = b;
//        results[2] = dmin;
//        results[3] = dmax;
        return new double[]{a,b};
    }
}