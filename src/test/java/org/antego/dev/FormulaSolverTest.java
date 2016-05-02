package org.antego.dev;

//import org.junit.Assert;

import org.junit.Test;

import static org.junit.Assert.*;


public class FormulaSolverTest {
    @Test
    public void testGetCoordinates() throws Exception {
        FormulaSolver fs = new FormulaSolver();
        fs.setVars(24, 31, 18, 9, 14, 0);
        double[][] coords = fs.getCoordinates(new double[][]{{0, 1}, {1, 31}, {2, 71}, {3, 240}, {4, 470}}, 0, 640, 480);
        Integer countNotNull = 0;
        for (double[] c : coords) if (c[2] != 0) countNotNull++;
        assertTrue("z coordinate is zero more than once", countNotNull > 1);
    }
}