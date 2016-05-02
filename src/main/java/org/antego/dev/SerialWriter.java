package org.antego.dev;

import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialWriter {
    //todo logger
    SerialPort serialPort;
    Controller controller;

    public SerialWriter(String port, Controller controller) throws SerialPortException {
        this.controller = controller;
        serialPort = new SerialPort(port);
        System.out.println("Port opened: " + serialPort.openPort());
        System.out.println("Params setted: " + serialPort.setParams(9600, 8, 1, 0, true, false));
        int mask = SerialPort.MASK_RXCHAR;
        serialPort.setEventsMask(mask);
        serialPort.addEventListener(new SerialPortReader());
    }

    public boolean disconnect() {
        try {
            serialPort.closePort();
            return true;
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean rotate(int steps) {
        try {
            return serialPort.writeString("r" + steps + "\r\n");
        } catch (SerialPortException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    class SerialPortReader implements SerialPortEventListener {
        StringBuilder message = new StringBuilder();

        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b : buffer) {
                        if ((b == '\r' || b == '\n') && message.length() > 0) {
                            if (message.toString().contentEquals("r:Done")) {
                                Thread.sleep(1000);
                                Platform.runLater(() -> controller.setTakeShoot(true));
                            }
                            System.out.println(message.toString());
                            message.setLength(0);
                        } else {
                            if (b != '\n' && b != '\r') message.append((char) b);
                        }
                    }
                } catch (SerialPortException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
