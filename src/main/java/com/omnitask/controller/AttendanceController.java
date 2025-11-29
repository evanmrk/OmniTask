package com.omnitask.controller;

import com.github.sarxos.webcam.Webcam;
import com.omnitask.model.Attendance;
import com.omnitask.model.Location;
import com.omnitask.service.AttendanceService;
import com.omnitask.util.TimeUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalTime;
import java.util.UUID;

public class AttendanceController {

    @FXML private Label lblEmployeeName;
    @FXML private Label lblEmployeeId;
    @FXML private Label lblCurrentTime;
    @FXML private Label lblWorkPeriod;
    @FXML private Label lblStatus;
    @FXML private TextField txtLatitude;
    @FXML private TextField txtLongitude;
    @FXML private ImageView imgWebcam;
    @FXML private Button btnCapture;
    @FXML private Button btnCheckIn;
    @FXML private Button btnCheckOut;
    @FXML private VBox vboxResult;
    @FXML private Label lblResult;
    @FXML private ProgressIndicator progressIndicator;

    private AttendanceService attendanceService;
    private Webcam webcam;
    private String currentEmployeeId = "EMP001"; // Demo employee
    private String capturedPhotoPath;
    private boolean photoCapture = false;

    @FXML
    public void initialize() {
        attendanceService = new AttendanceService();

        // Set employee info (in real app, get from login)
        lblEmployeeId.setText(currentEmployeeId);
        lblEmployeeName.setText("John Doe");

        // Initialize webcam
        initializeWebcam();

        // Update time periodically
        startTimeUpdater();

        // Get current location (simulate GPS)
        simulateGPS();

        vboxResult.setVisible(false);
        progressIndicator.setVisible(false);
    }

    private void initializeWebcam() {
        new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.open();
                startWebcamStream();
            } else {
                Platform.runLater(() -> {
                    showAlert("Webcam Error", "No webcam detected!", Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void startWebcamStream() {
        Thread thread = new Thread(() -> {
            while (webcam != null && webcam.isOpen() && !photoCapture) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        Platform.runLater(() -> {
                            imgWebcam.setImage(SwingFXUtils.toFXImage(image, null));
                        });
                    }
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void startTimeUpdater() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Platform.runLater(() -> {
                        LocalTime now = LocalTime.now();
                        lblCurrentTime.setText(now.toString());
                        lblWorkPeriod.setText(TimeUtil.getCurrentWorkPeriod(now));
                    });
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void simulateGPS() {
        // In real app, use actual GPS library
        // For now, simulate Medan coordinates with small variance
        double baseLat = 3.5952;
        double baseLon = 98.6722;
        double variance = 0.001;

        txtLatitude.setText(String.format("%.6f", baseLat + (Math.random() * variance)));
        txtLongitude.setText(String.format("%.6f", baseLon + (Math.random() * variance)));
    }

    @FXML
    private void handleCapture() {
        if (webcam == null || !webcam.isOpen()) {
            showAlert("Error", "Webcam not available!", Alert.AlertType.ERROR);
            return;
        }

        photoCapture = true;
        BufferedImage image = webcam.getImage();

        if (image != null) {
            try {
                // Save captured image
                File photoDir = new File("captured_photos");
                photoDir.mkdirs();

                capturedPhotoPath = "captured_photos/" + currentEmployeeId + "_" +
                        UUID.randomUUID().toString() + ".jpg";
                File photoFile = new File(capturedPhotoPath);
                ImageIO.write(image, "JPG", photoFile);

                imgWebcam.setImage(SwingFXUtils.toFXImage(image, null));

                showAlert("Success", "Photo captured successfully!", Alert.AlertType.INFORMATION);
                btnCheckIn.setDisable(false);
                btnCheckOut.setDisable(false);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to capture photo: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }

        photoCapture = false;
    }

    @FXML
    private void handleCheckIn() {
        if (capturedPhotoPath == null) {
            showAlert("Error", "Please capture a photo first!", Alert.AlertType.WARNING);
            return;
        }

        progressIndicator.setVisible(true);
        vboxResult.setVisible(false);

        new Thread(() -> {
            try {
                double lat = Double.parseDouble(txtLatitude.getText());
                double lon = Double.parseDouble(txtLongitude.getText());
                Location location = new Location(lat, lon);

                Attendance attendance = attendanceService.checkIn(
                        currentEmployeeId, location, capturedPhotoPath
                );

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    vboxResult.setVisible(true);

                    String message = "Check-in successful!\n" +
                            "Time: " + attendance.getCheckInTime().toLocalTime() + "\n";

                    if (attendance.isLate()) {
                        message += "Late by: " + attendance.getLateMinutes() + " minutes\n";
                        lblResult.setStyle("-fx-text-fill: #ff6b6b;");
                    } else {
                        message += "On time!\n";
                        lblResult.setStyle("-fx-text-fill: #51cf66;");
                    }

                    lblResult.setText(message);
                    lblStatus.setText("Status: CHECKED IN");
                    lblStatus.setStyle("-fx-background-color: #51cf66; -fx-text-fill: white;");

                    btnCheckIn.setDisable(true);
                    capturedPhotoPath = null;
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Check-in Failed", e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void handleCheckOut() {
        if (capturedPhotoPath == null) {
            showAlert("Error", "Please capture a photo first!", Alert.AlertType.WARNING);
            return;
        }

        progressIndicator.setVisible(true);
        vboxResult.setVisible(false);

        new Thread(() -> {
            try {
                double lat = Double.parseDouble(txtLatitude.getText());
                double lon = Double.parseDouble(txtLongitude.getText());
                Location location = new Location(lat, lon);

                Attendance attendance = attendanceService.checkOut(
                        currentEmployeeId, location, capturedPhotoPath
                );

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    vboxResult.setVisible(true);

                    String message = "Check-out successful!\n" +
                            "Time: " + attendance.getCheckOutTime().toLocalTime() + "\n" +
                            "Status: " + attendance.getStatus();

                    lblResult.setText(message);
                    lblResult.setStyle("-fx-text-fill: #51cf66;");
                    lblStatus.setText("Status: CHECKED OUT");
                    lblStatus.setStyle("-fx-background-color: #868e96; -fx-text-fill: white;");

                    btnCheckOut.setDisable(true);
                    capturedPhotoPath = null;
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Check-out Failed", e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void handleRefreshGPS() {
        simulateGPS();
        showAlert("GPS Updated", "Location refreshed successfully!", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}