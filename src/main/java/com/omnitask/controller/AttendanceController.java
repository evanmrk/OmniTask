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
import javafx.stage.Modality; // Import Modality
import javafx.stage.Stage;
import javafx.scene.control.Button;

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
    @FXML private Button btnEmployees;

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

    /**
     * Method untuk mengembalikan sesi login saat kembali dari halaman lain.
     */
    public void restoreSession(Employee employee) {
        if (employee != null) {
            this.currentEmployee = employee;
            // Panggil showDashboard agar logika pengecekan Role (if manager...) JALAN LAGI
            showDashboard();
        } else {
            // Jika data hilang, paksa logout / kembali ke login
            paneDashboard.setVisible(false);
            paneLogin.setVisible(true);
        }
    }

    // 1. LOGIKA LOGIN
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

    // --- TAMBAHAN BARU: PINDAH KE REGISTER ---
    @FXML
    private void handleGoToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Registrasi Karyawan OmniTask");
            stage.setScene(new Scene(root));

            // Blokir window utama sampai register ditutup (Modal)
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Gagal membuka halaman registrasi.");
        }
    }

    // 2. LOGIKA LOGOUT
    @FXML
    private void handleLogout() {
        currentEmployee = null;
        paneDashboard.setVisible(false);
        paneLogin.setVisible(true);
        txtInputId.clear();
        lblResult.setText("");
    }

    // 3. TAMPILKAN DASHBOARD
    private void showDashboard() {
        // 1. Tampilkan Panel Dashboard, Sembunyikan Login
        paneLogin.setVisible(false);
        paneDashboard.setVisible(true);

        // 2. Set Data Text
        if (currentEmployee != null) {
            lblName.setText(currentEmployee.getName());
            lblTarget.setText(currentEmployee.getDailyTarget() != null ? currentEmployee.getDailyTarget() : "-");

            // 3. LOGIKA MANAGER
            String role = currentEmployee.getRole();
            System.out.println("Role User saat ini: " + role);

            if (role != null && role.toLowerCase().contains("manager")) {
                btnEmployees.setVisible(true);
                btnEmployees.setManaged(true);
            } else {
                btnEmployees.setVisible(false);
                btnEmployees.setManaged(false);
            }

            // 4. Load Foto
            if (currentEmployee.getPhotoPath() != null) {
                try {
                    File file = new File(currentEmployee.getPhotoPath());
                    if (file.exists()) {
                        imgProfile.setImage(new Image(file.toURI().toString()));
                    }
                } catch (Exception e) {
                    System.err.println("Gagal load foto: " + e.getMessage());
                }
            }
        }

        // 5. Cek Lokasi
        checkLocation();
    }

    @FXML
    private void handleGoToEmployees() {
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/EmployeePage.fxml");

            if (fxmlLocation == null) {
                throw new java.io.FileNotFoundException("File /fxml/EmployeePage.fxml tidak ditemukan!");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            // Pindah Scene
            Stage stage = (Stage) btnEmployees.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Gagal membuka halaman Employee: " + e.getMessage()).show();
        }
    }

    // 4. CEK LOKASI
    private void checkLocation() {
        // Simulasi Lokasi
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

    // 5. CHECK IN
    @FXML
    private void handleCheckIn() {
        processAttendance(true);
    }

    // 6. CHECK OUT
    @FXML
    private void handleCheckOut() {
        processAttendance(false);
    }

    private void processAttendance(boolean isCheckIn) {
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


    @FXML
    private void handleGoToTasks() {
        if (currentEmployee == null) {
            lblError.setText("Silakan Login terlebih dahulu!");
            return;
        }

        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/TaskPage.fxml");

            if (fxmlLocation == null) {
                System.err.println("ERROR: File tidak ditemukan di /fxml/TaskPage.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            javafx.scene.Parent root = loader.load();

            // Pindah Scene
            javafx.stage.Stage stage = (javafx.stage.Stage) btnCheckIn.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            lblResult.setText("Error: " + e.getMessage());
        }
    }
}