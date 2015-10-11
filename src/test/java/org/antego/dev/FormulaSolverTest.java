package org.antego.dev;

//import org.junit.Assert;

import org.junit.Test;

import static org.junit.Assert.*;


public class FormulaSolverTest {


    @Test
    public void testGetCoordinates() throws Exception {
        FormulaSolver fs = new FormulaSolver(null);
        fs.setVars(24, 31, 18, 9, 14, 0);
        double[][] coords = fs.getCoordinates(new double[]{1, 31, 71, 240, 470}, 0);
        Integer countNotNull = 0;
        for (double[] c : coords) if (c[2] != 0) countNotNull++;
        assertTrue("z coordinate more than 1 is null", countNotNull > 1);
    }

    @Test
    public void testTurnProfile() throws Exception {
        FormulaSolver fs = new FormulaSolver(null);
        fs.setVars(0, 0, 0, 0, 10, 0);
        double[] coords = new double[]{10, 10, 5};
        assertArrayEquals("kjk", new double[]{-10, 0, 5}, fs.turnProfile(new double[][]{{10, 10, 5}}, Math.toRadians(90))[0], 0.00001);

    }
}