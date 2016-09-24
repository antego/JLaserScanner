package com.github.antego.laserscanner;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.antego.laserscanner.ImageProcessor.*;

public class Controller implements Initializable {
    private final static Logger logger = Logger.getLogger(Controller.class.toString());

    private static final int STEPS_COUNT = 456;

    @FXML
    private Button startBtn;
    @FXML
    private TextField frameWidthFld, frameHeightFld, camIdFld;
    @FXML
    private Tab calibTab, detectTab;
    @FXML
    private ImageView currentFrame;
    @FXML
    private TextField fiField, thetaField, hField, alfaField, shaftXFld, shaftYFld, deltaAngleFld;
    @FXML
    private Button startScanBtn;
    @FXML
    private ComboBox<String> portNameComboBox;
    @FXML
    private CheckBox rawImgCheckbox;
    @FXML
    private TextField hueMinFld, hueMaxFld, satMinFld, satMaxFld, valMinFld, valMaxFld;

    private volatile boolean takeShoot;
    private double angle;
    private CaptureThread captureThread;
    private FileManager fileManager;
    private ImageProcessor imageProcessor = new ImageProcessor();
    private volatile boolean isScanning = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hueMinFld.setText(DEFAULT_HUE_MIN + "");
        hueMaxFld.setText(DEFAULT_HUE_MAX + "");
        satMinFld.setText(DEFAULT_SAT_MIN + "");
        satMaxFld.setText(DEFAULT_SAT_MAX + "");
        valMinFld.setText(DEFAULT_VAL_MIN + "");
        valMaxFld.setText(DEFAULT_VAL_MAX + "");

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
        if (hueMinFld.getText() != null &&
                hueMaxFld.getText() != null &&
                satMinFld.getText() != null &&
                satMaxFld.getText() != null &&
                valMinFld.getText() != null &&
                valMaxFld.getText() != null) {
            imageProcessor.setThresholds(Double.parseDouble(hueMinFld.getText()),
                    Double.parseDouble(hueMaxFld.getText()),
                    Double.parseDouble(satMinFld.getText()),
                    Double.parseDouble(satMaxFld.getText()),
                    Double.parseDouble(valMinFld.getText()),
                    Double.parseDouble(valMaxFld.getText()));
        }
    }

    @FXML
    protected void handleCameraButton(ActionEvent event) {
        if (captureThread == null || !captureThread.isAlive()) {
            startCamera();
            try {
                captureThread = new CaptureThread();
                captureThread.start();
            } catch (FrameBuffer.CameraNotOpenedException camEx) {
                stopCamera();
                Alert alert = new Alert(Alert.AlertType.ERROR, camEx.getMessage());
                alert.showAndWait();
            } catch (SerialPortException serialEx) {
                stopCamera();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error while opening the port. Please make sure that Arduino connected or try another port.");
                alert.showAndWait();
            }
        } else {
            stopCamera();
            if (captureThread != null) {
                captureThread.setCapture(false);
            }
        }
    }

    private void startCamera() {
        startBtn.setText("Stop Camera");
        startScanBtn.setDisable(false);
        detectTab.setDisable(false);
        calibTab.setDisable(true);
    }

    private void stopCamera() {
        startBtn.setText("Start Camera");
        startScanBtn.setDisable(true);
        detectTab.setDisable(true);
        calibTab.setDisable(false);
    }

    @FXML
    protected void startScan() {
        if (isScanning) {
            isScanning = false;
            startBtn.setDisable(false);
            deltaAngleFld.setDisable(false);
            startScanBtn.setText("Start scan");
            angle = 0;
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


    public void setTakeShoot(boolean ts) {
            takeShoot = ts;
    }

    public void onClose() {
        if (captureThread != null) {
            captureThread.setCapture(false);
        }
    }

    private class CaptureThread extends Thread {
        private FormulaSolver formulaSolver;
        private FrameBuffer frameBuffer;
        private SerialWriter serialWriter;
        private volatile boolean capture = true;

        public CaptureThread() throws FrameBuffer.CameraNotOpenedException, SerialPortException {
            try {
                frameBuffer = new FrameBuffer();
                serialWriter = new SerialWriter(Controller.this.portNameComboBox.getValue(), Controller.this);
            } catch (SerialPortException e) {
                frameBuffer.stop();
                throw e;
            }
            formulaSolver = new FormulaSolver();
        }

        @Override
        public void run() {
            if (!thetaField.getText().isEmpty() &&
                    !fiField.getText().isEmpty() &&
                    !hField.getText().isEmpty() &&
                    !alfaField.getText().isEmpty() &&
                    !shaftXFld.getText().isEmpty() &&
                    !shaftYFld.getText().isEmpty()) {
                formulaSolver.setVars(Double.parseDouble(thetaField.getText()),
                        Double.parseDouble(fiField.getText()),
                        Double.parseDouble(alfaField.getText()),
                        Double.parseDouble(hField.getText()),
                        Double.parseDouble(shaftXFld.getText()),
                        Double.parseDouble(shaftYFld.getText()));
            }

            while (capture) {
                Image tmp = grabFrame(frameBuffer);
                Platform.runLater(() -> {
                    if (captureThread != null)
                        currentFrame.setImage(tmp);
                });
            }
            frameBuffer.stop();
            serialWriter.disconnect();
        }

        private Image grabFrame(FrameBuffer fb) {
            Image imageToShow = null;
            try {
                BufferedImage frame = fb.getFrame();
                if (frame != null) {
                    double[][] coords = imageProcessor.findDots(frame);
                    imageToShow = SwingFXUtils.toFXImage(frame, null);
                    if (takeShoot && isScanning) {
                        double[][] fullCoords =
                                formulaSolver.getCoordinates(coords, angle, frame.getWidth(), frame.getHeight());
                        fileManager.appendToFile(fullCoords);
                        setTakeShoot(false);
                        nextScan();
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "", e);
            }
            return imageToShow;
        }

        private void nextScan() {
            int steps;
            if (!deltaAngleFld.getText().isEmpty()) {
                steps = Integer.parseInt(deltaAngleFld.getText());
            } else {
                return;
            }
            serialWriter.rotate(steps);
            angle += (double) steps * 360 / STEPS_COUNT;
        }

        public void setCapture(boolean capture) {
            this.capture = capture;
        }
    }
}