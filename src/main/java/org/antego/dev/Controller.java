package org.antego.dev;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.MatOfByte;
import org.opencv.core.CvType;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private Button start_btn;
    @FXML
    private TextField frameWidthFld;
    @FXML
    private TextField frameHeightFld;
    @FXML
    private TextField camIdFld;
    @FXML
    private ImageView currentFrame;
    //manual method fields
    @FXML
    private Pane manual_pane;
    @FXML
    private TextField fi_text;
    @FXML
    private TextField theta_text;
    @FXML
    private TextField h_text;
    @FXML
    private TextField alfa_fld;
    @FXML
    private TextField shaft_x_fld;
    @FXML
    private TextField shaft_y_fld;
    //auto method fields
    @FXML
    private TextField delta_angle_fld;
    @FXML
    private Button startScanBtn;
    @FXML
    private ComboBox<String> portNameComboBox;
    @FXML
    private Label overall_angle_lbl;
    @FXML
    private CheckBox rawImg_checkbox;
    @FXML
    private TextField hueMin1;
    @FXML
    private TextField hueMin2;
    @FXML
    private TextField hueMax1;
    @FXML
    private TextField hueMax2;
    @FXML
    private TextField satMin;
    @FXML
    private TextField satMax;
    @FXML
    private TextField valMin;
    @FXML
    private TextField valMax;

    private volatile boolean takeShoot;
    private Double angle = .0;
    private Pane rootElement;
    private Thread captureThread;
    private FileManager fileManager;
    private ImageProcessor imageProcessor = new ImageProcessor();
    private volatile boolean isScanning = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO choose ports
        manual_pane.setVisible(true);
        double[] thresholds = imageProcessor.getThresholds();
        hueMin1.setText(thresholds[0] + "");
        hueMax1.setText(thresholds[1] + "");
        hueMin2.setText(thresholds[2] + "");
        hueMax2.setText(thresholds[3] + "");
        satMin.setText(thresholds[4] + "");
        satMax.setText(thresholds[5] + "");
        valMin.setText(thresholds[6] + "");

        portNameComboBox.setOnShowing(new EventHandler<Event>() {
            public void handle(Event event) {
                portNameComboBox.getItems().clear();
                portNameComboBox.getItems().addAll(SerialPortList.getPortNames());
            }
        });
    }


    @FXML
    protected void rawImgClicked(ActionEvent event) {
        imageProcessor.setShowRawImg(rawImg_checkbox.isSelected());
    }

    @FXML
    protected void applyThresholdsClick(ActionEvent event) {
        if (hueMin1.getText() != null && hueMin2 != null && hueMax1 != null && hueMax2 != null && satMin != null && satMax != null && valMin != null && valMax != null) {
            imageProcessor.setThresholds(Double.parseDouble(hueMin1.getText()), Double.parseDouble(hueMax1.getText()), Double.parseDouble(hueMin2.getText()), Double.parseDouble(hueMax2.getText()), Double.parseDouble(satMin.getText()), Double.parseDouble(satMax.getText()), Double.parseDouble(valMin.getText()), Double.parseDouble(valMax.getText()));
        }
    }


    @FXML
    protected void startCamera(ActionEvent event) {
        // check: the main class is accessible?
        if (rootElement != null) {
            start_btn.setText("Stop Camera");
            manual_pane.setDisable(true);
            // get the ImageView object for showing the video stream
            if (captureThread == null || !captureThread.isAlive()) {
                try {
                    captureThread = new CaptureThread();
                    captureThread.start();
                } catch (FrameBuffer.CameraNotOpenedException camEx) {
                    start_btn.setText("Start Camera");
                    Alert alert = new Alert(Alert.AlertType.ERROR, camEx.getMessage());
                    alert.showAndWait();
                } catch (SerialPortException serialEx) {
                    start_btn.setText("Start Camera");
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error while opening the port. Please make sure that Arduino connected or try another port.");
                    alert.showAndWait();
                }
            } else {
                start_btn.setText("Start Camera");
                manual_pane.setDisable(false);
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
            start_btn.setDisable(false);
            delta_angle_fld.setDisable(false);
            startScanBtn.setText("Start scan");
            angle = 0.0;
        } else {
            if (delta_angle_fld.getText().isEmpty()) {
                return;
            }
            start_btn.setDisable(true);
            delta_angle_fld.setDisable(true);
            startScanBtn.setText("Stop scan");
            fileManager = new FileManager();
            isScanning = true;
        }
    }


    public synchronized void setTakeShoot(boolean ts) {
            takeShoot = ts;
    }


    private Image mat2Image(Mat frame) {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the frame in the buffer
        Highgui.imencode(".bmp", frame, buffer);
        // build and return an Image created from the image encoded in the buffer
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
            frameBuffer = new FrameBuffer(Integer.parseInt(camIdFld.getText()),Integer.parseInt(frameWidthFld.getText()), Integer.parseInt(frameHeightFld.getText()));
            serialWriter = new SerialWriter(Controller.this.portNameComboBox.getValue(), Controller.this);
            formulaSolver = new FormulaSolver();
        }

        @Override
        public void run() {
            if (!theta_text.getText().isEmpty() && !fi_text.getText().isEmpty() && !h_text.getText().isEmpty() && !alfa_fld.getText().isEmpty() && !shaft_x_fld.getText().isEmpty() && !shaft_y_fld.getText().isEmpty())
                formulaSolver.setVars(Double.parseDouble(theta_text.getText()), Double.parseDouble(fi_text.getText()), Double.parseDouble(alfa_fld.getText()), Double.parseDouble(h_text.getText()), Double.parseDouble(shaft_x_fld.getText()), Double.parseDouble(shaft_y_fld.getText()));
            while (!interrupted()) {
                tmp = grabFrame(frameBuffer);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (captureThread != null)
                            currentFrame.setImage(tmp);
                    }
                });
            }
            frameBuffer.stop();
            serialWriter.disconnect();
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
                boolean rotated = false;
                if (takeShoot && isScanning) {
                    nextScan();
                    rotated = true;
                }
                // if the frame is not empty, process it
                if (!frame.empty()) {
                    coords = imageProcessor.findDots(frame);
                    imageToShow = mat2Image(frame);
                    if (takeShoot) {
                        if (!rotated && isScanning)
                            nextScan();
                        fullCoords = formulaSolver.getCoordinates(coords, angle);
                        if (fullCoords != null)
                            fileManager.appendToFile(fullCoords);
                        else System.out.println("Null full coordinates");
                        takeShoot = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // log the error
                System.err.println("ERROR: " + e.getMessage());
            } finally {

            }
            return imageToShow;
        }

        private void nextScan() {
            int steps = 0;
            if (!delta_angle_fld.getText().isEmpty()) {
                steps = Integer.parseInt(delta_angle_fld.getText());
            } else {
                return;
            }
            serialWriter.rotate(steps);
            angle += (double) steps * 360 / 456;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    overall_angle_lbl.setText(angle.toString());
                }
            });
        }
    }
}