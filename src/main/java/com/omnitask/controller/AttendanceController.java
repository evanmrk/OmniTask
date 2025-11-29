package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.Location;
import com.omnitask.service.AttendanceService;
import com.omnitask.service.GeofenceService;
import com.omnitask.service.SPARQLService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class AttendanceController {

    // --- UI LOGIN ---
    @FXML private VBox paneLogin;
    @FXML private TextField txtInputId;
    @FXML private Label lblError;

    // --- UI DASHBOARD ---
    @FXML private BorderPane paneDashboard;
    @FXML private Label lblName;
    @FXML private Label lblTarget;
    @FXML private ImageView imgProfile;
    @FXML private Label lblLocationStatus;
    @FXML private Label lblResult;
    @FXML private Button btnCheckIn;
    @FXML private Button btnCheckOut;

    // --- SERVICES ---
    private SPARQLService sparqlService;
    private AttendanceService attendanceService;
    private GeofenceService geofenceService;
    private Employee currentEmployee;

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();
        attendanceService = new AttendanceService();
        geofenceService = new GeofenceService();
    }

    // ==========================================
    // 1. LOGIKA NAVIGASI (LOGIN & REGISTER)
    // ==========================================

    @FXML
    private void handleLogin() {
        String id = txtInputId.getText().trim();
        if (id.isEmpty()) {
            lblError.setText("Masukkan ID dulu!"); return;
        }

        Employee emp = sparqlService.getEmployeeById(id);
        if (emp != null) {
            currentEmployee = emp;
            showDashboard();
            lblError.setText("");
        } else {
            lblError.setText("ID tidak ditemukan!");
        }
    }

    @FXML
    private void handleLogout() {
        currentEmployee = null;
        paneDashboard.setVisible(false);
        paneLogin.setVisible(true);
        txtInputId.clear();
        lblResult.setText("");
    }

    @FXML
    private void handleGoToRegister() {
        try {
            // Load Halaman Registrasi
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterView.fxml"));
            Parent registerRoot = loader.load();
            
            // Pindah Scene
            Stage stage = (Stage) txtInputId.getScene().getWindow();
            stage.setScene(new Scene(registerRoot, 600, 700));
            stage.setTitle("OmniTask - Registrasi Karyawan");
            
        } catch (IOException e) {
            e.printStackTrace();
            lblError.setText("Gagal memuat halaman registrasi.");
        }
    }

    // ==========================================
    // 2. LOGIKA DASHBOARD
    // ==========================================

    private void showDashboard() {
        paneLogin.setVisible(false);
        paneDashboard.setVisible(true);

        lblName.setText(currentEmployee.getName());
        lblTarget.setText(currentEmployee.getDailyTarget());

        // LOAD FOTO (PORTABLE)
        try {
            String path = currentEmployee.getPhotoPath();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    imgProfile.setImage(new Image(file.toURI().toString()));
                }
            } else {
                imgProfile.setImage(null);
            }
        } catch (Exception e) {
            System.out.println("Gagal load foto: " + e.getMessage());
        }

        checkLocation();
    }

    // ==========================================
    // 3. LOGIKA ABSENSI (LOCATION & CHECK IN/OUT)
    // ==========================================

    private void checkLocation() {
        // Simulasi Lokasi (Koordinat Medan)
        Location loc = new Location(3.5952, 98.6722);
        boolean isOffice = geofenceService.isWithinGeofence(loc);

        if (isOffice) {
            lblLocationStatus.setText("Lokasi: Di Kantor (Aman)");
            lblLocationStatus.setStyle("-fx-text-fill: green;");
            btnCheckIn.setDisable(false);
            btnCheckOut.setDisable(false);
        } else {
            lblLocationStatus.setText("Lokasi: Diluar Kantor!");
            lblLocationStatus.setStyle("-fx-text-fill: red;");
            btnCheckIn.setDisable(true);
            btnCheckOut.setDisable(true);
        }
    }

    @FXML
    private void handleCheckIn() {
        processAttendance(true);
    }

    @FXML
    private void handleCheckOut() {
        processAttendance(false);
    }

    private void processAttendance(boolean isCheckIn) {
        // Jalankan di thread terpisah agar UI tidak macet
        new Thread(() -> {
            try {
                Location loc = new Location(3.5952, 98.6722);
                
                if (isCheckIn) {
                    attendanceService.checkIn(currentEmployee.getId(), loc, currentEmployee.getPhotoPath());
                } else {
                    attendanceService.checkOut(currentEmployee.getId(), loc, currentEmployee.getPhotoPath());
                }

                Platform.runLater(() -> {
                    lblResult.setText(isCheckIn ? "Check-In Berhasil!" : "Check-Out Berhasil!");
                    lblResult.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    if(isCheckIn) btnCheckIn.setDisable(true);
                    else btnCheckOut.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblResult.setText("Gagal: " + e.getMessage());
                    lblResult.setStyle("-fx-text-fill: red;");
                });
            }
        }).start();
    }
}