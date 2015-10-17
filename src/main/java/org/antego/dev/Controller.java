package org.antego.dev;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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
    @FXML
    private RadioButton manual_rd;
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
    private Pane auto_pane;
    @FXML
    private TextField delta_angle_fld;
    @FXML
    private Button startScanBtn;
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
    private FormulaSolver formulaSolver = new FormulaSolver();
    private FileManager fileManager = new FileManager();
    private ImageProcessor imageProcessor = new ImageProcessor();
    private volatile boolean isScanning = false;
    private boolean motionDetection;
    private Mat oldFrame;
    private Mat motionSumm;
    private Mat mask;

    private boolean first = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //TODO choose ports
        SerialWriter.setConnection("/dev/ttyACM0", this);
        auto_pane.setVisible(false);
        manual_pane.setVisible(true);
        manual_rd.setSelected(true);
        double[] thresholds = imageProcessor.getThresholds();
        hueMin1.setText(thresholds[0] + "");
        hueMax1.setText(thresholds[1] + "");
        hueMin2.setText(thresholds[2] + "");
        hueMax2.setText(thresholds[3] + "");
        satMin.setText(thresholds[4] + "");
        satMax.setText(thresholds[5] + "");
        valMin.setText(thresholds[6] + "");
    }

    @FXML
    protected void modeManual(ActionEvent event) {
        auto_pane.setVisible(false);
        manual_pane.setVisible(true);
        formulaSolver.setMode(true);
    }

    @FXML
    protected void modeAuto(ActionEvent event) {
        manual_pane.setVisible(false);
        auto_pane.setVisible(true);
        formulaSolver.setMode(false);
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
                    captureThread = new Thread() {
                        Image tmp;

                        FrameBuffer frameBuffer = new FrameBuffer(Integer.parseInt(camIdFld.getText()),Integer.parseInt(frameWidthFld.getText()), Integer.parseInt(frameHeightFld.getText()));

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
                        }
                    };
                    captureThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                start_btn.setText("Start Camera");
                manual_pane.setDisable(false);
                auto_pane.setDisable(true);
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
                if (motionDetection) {
                    if (first)
                        SerialWriter.rotate(456);
                    mask = getMotions(frame);
                }
                coords = imageProcessor.findDots(frame,mask);
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

    private Mat getMotions(Mat frame) {
        Mat localMotionFrame = new Mat(frame.size(), frame.type());
        Mat currentMotionFrame = frame.clone();
        if (first) {
            oldFrame = frame.clone();
            motionSumm = new Mat(frame.size(), CvType.CV_8UC1, new Scalar(0));
            first = false;
        }
        Core.absdiff(oldFrame, currentMotionFrame, localMotionFrame);
        oldFrame = currentMotionFrame.clone();
        Imgproc.cvtColor(localMotionFrame, localMotionFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(localMotionFrame, localMotionFrame, 20, 255, Imgproc.THRESH_BINARY);

//        Imgproc.erode(localMotionFrame, localMotionFrame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//        Imgproc.dilate(localMotionFrame, localMotionFrame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//
//        Imgproc.dilate(localMotionFrame, localMotionFrame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
//        Imgproc.erode(localMotionFrame, localMotionFrame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

        Core.add(motionSumm, localMotionFrame, motionSumm);
        Imgproc.erode(motionSumm, motionSumm, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Imgproc.dilate(motionSumm, motionSumm, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//
        Imgproc.dilate(motionSumm, motionSumm, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.erode(motionSumm, motionSumm, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        return motionSumm;
    }

    @FXML
    protected void takeShoot() {
        int steps = 1;
        if (!delta_angle_fld.getText().isEmpty()) {
            steps = Integer.parseInt(delta_angle_fld.getText());
        }
        SerialWriter.rotate(steps);
        angle += (double) steps * 360 / 456;
        overall_angle_lbl.setText(angle.toString());
    }

    @FXML
    protected void startScan() {
        if (isScanning) {
            isScanning = false;
            start_btn.setDisable(false);
            delta_angle_fld.setDisable(false);
            startScanBtn.setText("Start scan");
            angle = 0.0;
            first = true;
        } else {
            if (delta_angle_fld.getText().isEmpty()) {
                return;
            }
            start_btn.setDisable(true);
            delta_angle_fld.setDisable(true);
            startScanBtn.setText("Stop scan");
            motionDetection = true;
        }
    }

    private void nextScan() {
        int steps = 0;
        if (!delta_angle_fld.getText().isEmpty()) {
            steps = Integer.parseInt(delta_angle_fld.getText());
        } else {
            return;
        }
        SerialWriter.rotate(steps);
        angle += (double) steps * 360 / 456;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                overall_angle_lbl.setText(angle.toString());
            }
        });
    }


    public synchronized void setTakeShoot(boolean ts) {
        if (motionDetection) {
            motionDetection = false;
            isScanning = true;
            takeShoot = true;
            Highgui.imwrite("Mask.png",mask);
        } else {
            takeShoot = ts;
        }
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
}