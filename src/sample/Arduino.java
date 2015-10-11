package sample;

import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Created by anton on 31.03.2015.
 */
public class Arduino {
    private static Arduino arduino;
    private SerialPort serialPort;
    private Controller controller;

    private Arduino() {
    }

    public synchronized static Arduino getInstance() {
        if(arduino == null) {
            arduino = new Arduino();
        }
        return arduino;
    }

    public boolean setConnection(String port) {
        serialPort = new SerialPort(port);
        try {
            System.out.println("Port opened: " + serialPort.openPort());
            System.out.println("Params setted: " + serialPort.setParams(9600, 8, 1, 0, true, false));
            int mask = SerialPort.MASK_RXCHAR;
            serialPort.setEventsMask(mask);
            serialPort.addEventListener(new SerialPortReader());
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }
        //ccontroller.setPortLabel("Connected");
        return true;
    }

    public void registerController(Controller controller) {
        this.controller = controller;
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
                                Thread.sleep(500);
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

}
