package org.antego.dev;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by anton on 15.03.2015.
 */
public class FileManager {
    Path path;
    String fileName = "scan";
    int number;
//    File file;

    FileManager() {
        createFile();
    }

    public static void main(String... args) {
        FileManager fm = new FileManager();
        fm.appendToFile(new double[][]{{23}});
        fm.appendToFile(new double[][]{{23}});
        fm.appendToFile(new double[][]{{23}});
    }

    private void createFile() {
        try {
            path = Paths.get(fileName + number+".XYZ");

//            Files.createDirectories(path.getParent());

            try {
                Files.createFile(path);
            } catch (FileAlreadyExistsException e) {
                System.err.println("already exists: " + e.getMessage());
                number++;
                createFile();
            }
        } catch (IOException e) {
            System.out.println("Error creating file");
            e.printStackTrace();
        }
    }

    public void appendToFile(double[][] coords) {
        StringBuilder sb = new StringBuilder();
        if(Files.exists(path)) {
            try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path.toString(), true)))) {
                for(double[] c: coords) {
                    sb.append(c[0]).append(" ").append(c[1]).append(" ").append(c[2]).append("\n");
                }
                out.print(sb);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        }
    }
}
