package org.antego.dev;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.control.Alert;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class Controller implements Initializable {
    @FXML
    private Button startBtn;
    @FXML
    private TextField frameWidthFld;
    @FXML
    private TextField frameHeightFld;
    @FXML
    private TextField camIdFld;
    @FXML
    private Tab calibTab;
    @FXML
    private Tab detectTab;
    @FXML
    private ImageView currentFrame;
    @FXML
    private TextField fiField;
    @FXML
    private TextField thetaField;
    @FXML
    private TextField hField;
    @FXML
    private TextField alfaField;
    @FXML
    private TextField shaftXFld;
    @FXML
    private TextField shaftYFld;
    @FXML
    private TextField deltaAngleFld;
    @FXML
    private Button startScanBtn;
    @FXML
    private ComboBox<String> portNameComboBox;
    @FXML
    private CheckBox rawImgCheckbox;
    @FXML
    private TextField hueMin1Fld;
    @FXML
    private TextField hueMin2Fld;
    @FXML
    private TextField hueMax1Fld;
    @FXML
    private TextField hueMax2Fld;
    @FXML
    private TextField satMinFld;
    @FXML
    private TextField satMaxFld;
    @FXML
    private TextField valMinFld;
    @FXML
    private TextField valMaxFld;

    private volatile boolean takeShoot;
    private Double angle = .0;
    private Pane rootElement;
    private Thread captureThread;
    private FileManager fileManager;
    private ImageProcessor imageProcessor = new ImageProcessor();
    private volatile boolean isScanning = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        double[] thresholds = imageProcessor.getThresholds();
        hueMin1Fld.setText(thresholds[0] + "");
        hueMax1Fld.setText(thresholds[1] + "");
        hueMin2Fld.setText(thresholds[2] + "");
        hueMax2Fld.setText(thresholds[3] + "");
        satMinFld.setText(thresholds[4] + "");
        satMaxFld.setText(thresholds[5] + "");
        valMinFld.setText(thresholds[6] + "");

        portNameComboBox.setOnShowing(event -> {
                portNameComboBox.getItems().clear();
                portNameComboBox.getItems().addAll(SerialPortList.getPortNames());
            }
        );
    }


    @FXML
    protected void rawImgClicked(ActionEvent event) {
        imageProcessor.setShowRawImg(rawImgCheckbox.isSelected());
    }

    @FXML
    protected void applyThresholdsClick(ActionEvent event) {
        if (hueMin1Fld.getText() != null && hueMin2Fld != null && hueMax1Fld != null && hueMax2Fld != null && satMinFld != null && satMaxFld != null && valMinFld != null && valMaxFld != null) {
            imageProcessor.setThresholds(Double.parseDouble(hueMin1Fld.getText()), Double.parseDouble(hueMax1Fld.getText()), Double.parseDouble(hueMin2Fld.getText()), Double.parseDouble(hueMax2Fld.getText()), Double.parseDouble(satMinFld.getText()), Double.parseDouble(satMaxFld.getText()), Double.parseDouble(valMinFld.getText()), Double.parseDouble(valMaxFld.getText()));
        }
    }


    @FXML
    protected void startCamera(ActionEvent event) {
        // check: the main class is accessible?
        if (rootElement != null) {
            // get the ImageView object for showing the video stream
            if (captureThread == null || !captureThread.isAlive()) {
                startBtn.setText("Stop Camera");
                startScanBtn.setDisable(false);
                detectTab.setDisable(false);
                calibTab.setDisable(true);
                try {
                    captureThread = new CaptureThread();
                    captureThread.start();
                } catch (FrameBuffer.CameraNotOpenedException camEx) {
                    startBtn.setText("Start Camera");
                    startScanBtn.setDisable(true);
                    detectTab.setDisable(true);
                    calibTab.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR, camEx.getMessage());
                    alert.showAndWait();
                } catch (SerialPortException serialEx) {
                    startBtn.setText("Start Camera");
                    startScanBtn.setDisable(true);
                    detectTab.setDisable(true);
                    calibTab.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error while opening the port. Please make sure that Arduino connected or try another port.");
                    alert.showAndWait();
                }
            } else {
                startBtn.setText("Start Camera");
                startScanBtn.setDisable(true);
                detectTab.setDisable(true);
                calibTab.setDisable(false);
                // stop the timer
                if (captureThread != null) {
                    captureThread.interrupt();
                    captureThread = null;
                }
                // release the camera
                // clear the image container
//                currentFrame.setImage(null);
            }
        }
    }

    @FXML
    protected void startScan() {
        if (isScanning) {
            isScanning = false;
            startBtn.setDisable(false);
            deltaAngleFld.setDisable(false);
            startScanBtn.setText("Start scan");
            angle = 0.0;
        } else {
            if (deltaAngleFld.getText().isEmpty()) {
                return;
            }
            startBtn.setDisable(true);
            deltaAngleFld.setDisable(true);
            startScanBtn.setText("Stop scan");
            detectTab.setDisable(true);
            fileManager = new FileManager();
            isScanning = true;
            setTakeShoot(true);
        }
    }


    public synchronized void setTakeShoot(boolean ts) {
            takeShoot = ts;
    }


    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Highgui.imencode(".bmp", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public void setRootElement(Pane root) {
        rootElement = root;
    }

    private class CaptureThread extends Thread {
        Image tmp;

        private FormulaSolver formulaSolver;
        private FrameBuffer frameBuffer;
        private SerialWriter serialWriter;

        public CaptureThread() throws FrameBuffer.CameraNotOpenedException, SerialPortException {
//            try {
                frameBuffer = new FrameBuffer(Integer.parseInt(camIdFld.getText()),
                        Integer.parseInt(frameWidthFld.getText()),
                        Integer.parseInt(frameHeightFld.getText()));
                //serialWriter = new SerialWriter(Controller.this.portNameComboBox.getValue(), Controller.this);
//            } catch (SerialPortException e) {
//                frameBuffer.stop();
//                throw e;
//            }
            formulaSolver = new FormulaSolver();
        }

        @Override
        public void run() {
            if (!thetaField.getText().isEmpty() && !fiField.getText().isEmpty() && !hField.getText().isEmpty() && !alfaField.getText().isEmpty() && !shaftXFld.getText().isEmpty() && !shaftYFld.getText().isEmpty())
                formulaSolver.setVars(Double.parseDouble(thetaField.getText()),
                        Double.parseDouble(fiField.getText()),
                        Double.parseDouble(alfaField.getText()),
                        Double.parseDouble(hField.getText()),
                        Double.parseDouble(shaftXFld.getText()),
                        Double.parseDouble(shaftYFld.getText()));
            while (!interrupted()) {
                tmp = grabFrame(frameBuffer);
                Platform.runLater(() -> {
                    if (captureThread != null)
                        currentFrame.setImage(tmp);
                });
            }
            frameBuffer.stop();
            //serialWriter.disconnect();
        }

        private Image grabFrame(FrameBuffer fb) {
            //init
            Image imageToShow = null;
            Mat frame;

            final double[] coords;
            double[][] fullCoords;
            // check if the capture is open
            try {
                // read the current frame
                frame = fb.getFrame();
                if (!frame.empty()) {
                    coords = imageProcessor.findDots(frame);
                    imageToShow = mat2Image(frame);
                    if (takeShoot && isScanning) {
                        fullCoords = formulaSolver.getCoordinates(coords, angle);
                        if (fullCoords != null)
                            fileManager.appendToFile(fullCoords);
                        else
                            System.out.println("Null full coordinates");
                        setTakeShoot(false);
                        nextScan();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // log the error
                System.err.println("ERROR: " + e.getMessage());
            }
            return imageToShow;
        }

        private void nextScan() {
            int steps = 0;
            if (!deltaAngleFld.getText().isEmpty()) {
                steps = Integer.parseInt(deltaAngleFld.getText());
            } else {
                return;
            }
            serialWriter.rotate(steps);
            angle += (double) steps * 360 / 456;
        }
    }

    public void onClose() {
        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }
    }
}