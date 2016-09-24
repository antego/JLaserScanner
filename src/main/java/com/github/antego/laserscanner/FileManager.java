package com.github.antego.laserscanner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FileManager {
    private static final Logger logger = Logger.getLogger(FileManager.class.toString());

    private static final String FILE_NAME = "scan";
    private Path path;

    public FileManager() {
        int number = 0;
        path = Paths.get(FILE_NAME + number + ".XYZ");
        while (true) {
            if (Files.notExists(path)) {
                try {
                    Files.createFile(path);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Exception while creating a file", e);
                }
            } else {
                number++;
            }
        }
    }

    public void appendToFile(double[][] coords) {
        if (Files.exists(path)) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path.toString(), true)))) {
                StringBuilder sb = new StringBuilder();
                for (double[] c : coords) {
                    sb.append(c[0]).append(" ").append(c[1]).append(" ").append(c[2]);
                }
                out.println(sb);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception while writing to a file", e);
            }
        }
    }
}
