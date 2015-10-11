package sample; /**
 * Created by anton on 28.02.2015.
 */

import javafx.application.Platform;
import jssc.*;

public class SerialWriter {
    static SerialPort serialPort;
    static Controller controller;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String[] portNames = SerialPortList.getPortNames();
        for (int i = 0; i < portNames.length; i++) {
            System.out.println(portNames[i]);
        }
    }

//    public static void setController(Controller controller) {
//        ccontroller = controller;
//    }

    public static boolean disconnect() {
        try {
            serialPort.closePort();
            //ccontroller.setPortLabel("Disconnected");
            return true;
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean scan() {
        try {
            return serialPort.writeString("s");
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    public static boolean stopScan() {
        try {
            return serialPort.writeString("c");
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    public static boolean rotate(int steps) {
        try {
            return serialPort.writeString("r" + steps + "\r\n");
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setConnection(String port, Controller controller) {
        SerialWriter.controller = controller;
        serialPort = new SerialPort(port);
        try {
            System.out.println("Port opened: " + serialPort.openPort());
            System.out.println("Params setted: " + serialPort.setParams(9600, 8, 1, 0, true, false));
            int mask = SerialPort.MASK_RXCHAR;
            serialPort.setEventsMask(mask);
            serialPort.addEventListener(new SerialPortReader());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        //ccontroller.setPortLabel("Connected");
        return true;
    }

    static class SerialPortReader implements SerialPortEventListener {
        StringBuilder message = new StringBuilder();

        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b : buffer) {
                        if ((b == '\r' || b == '\n') && message.length() > 0) {
                            if (message.toString().contentEquals("r:Done")) {
                                Thread.sleep(1000);
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        controller.setTakeShoot(true);
                                    }
                                });
                            }
                            System.out.println(message.toString());
                            message.setLength(0);
                        } else {
                            if (b != '\n' && b != '\r') message.append((char) b);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                    System.out.println("serialEvent");
                }
            }
        }

    }


    public static String[] getPorts() {
        String[] portNames = SerialPortList.getPortNames();

        for (int i = 0; i < portNames.length; i++) {
            System.out.println(portNames[i]);
        }
        return portNames;
    }
}
