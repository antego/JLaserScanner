package org.antego.dev;

/**
 * Created by anton on 12.03.2015.
 */
public class Measure {
    Double distantion;
    Double x;

    public Measure(Double distantion, Double x) {
        this.distantion = distantion;
        this.x = x;
    }

    public Double getX() {

        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getDistantion() {
        return distantion;
    }

    public void setDistantion(Double distantion) {
        this.distantion = distantion;
    }
}
