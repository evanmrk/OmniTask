package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.Location;
import com.omnitask.service.AttendanceService;
import com.omnitask.service.GeofenceService;
import com.omnitask.service.SPARQLService;
import com.omnitask.service.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import com.omnitask.model.Task;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.omnitask.controller.ManagerLeaveController;


public class AttendanceController {

    // --- UI LOGIN ---
    @FXML private VBox paneLogin;
    @FXML private TextField txtInputId;
    @FXML private Label lblError;

    // --- UI DASHBOARD ---
    @FXML private BorderPane paneDashboard;
    @FXML private Label lblName;
    @FXML private Label lblTarget;
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

        // --- LOGIKA BARU: CEK SESI OTOMATIS ---
        if (SessionManager.isLoggedIn()) {
            // Jika di dompet ada ID, ambil datanya & langsung ke Dashboard
            this.currentEmployee = SessionManager.getCurrentUser();
            showDashboard();
        } else {
            // Jika kosong, tampilkan Login
            showLoginState();
        }
    }

    private void showLoginState() {
        paneDashboard.setVisible(false);
        paneLogin.setVisible(true);
        txtInputId.clear();
        lblResult.setText("");
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
        // ... validasi kosong ...

        Employee emp = sparqlService.getEmployeeById(id);
        if (emp != null) {
            // SIMPAN KE SESSION MANAGER
            SessionManager.login(emp);

            this.currentEmployee = emp;
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
        // HAPUS SESI
        SessionManager.logout();

        this.currentEmployee = null;
        showLoginState();
    }

    // 3. TAMPILKAN DASHBOARD
    private void showDashboard() {
        paneLogin.setVisible(false);
        paneDashboard.setVisible(true);

        if (currentEmployee != null) {
            lblName.setText(currentEmployee.getName());

            // --- BAGIAN INI DIGANTI DENGAN LOGIKA DINAMIS ---
            // Dulu: lblTarget.setText(currentEmployee.getDailyTarget() ... );
            // Sekarang: Panggil method khusus untuk load tasks
            updateDashboardTasks();
            // -----------------------------------------------

            // LOGIKA MANAGER
            String role = currentEmployee.getRole();
            if (role != null && role.toLowerCase().contains("manager")) {
                btnEmployees.setVisible(true);
                btnEmployees.setManaged(true);
            } else {
                btnEmployees.setVisible(false);
                btnEmployees.setManaged(false);
            }
        }

        updateAttendanceStatusUI();
        // 5. Cek Lokasi
        checkLocation();
    }

    private void updateDashboardTasks() {
        try {
            // 1. Ambil semua tugas milik karyawan ini
            List<Task> myTasks = sparqlService.getTasksForEmployee(currentEmployee.getId());

            StringBuilder activeTasksList = new StringBuilder();
            int count = 0;

            for (Task t : myTasks) {
                // 2. FILTER: Hanya ambil tugas yang BELUM SELESAI (Bukan COMPLETED)
                if (t.getStatus() != Task.TaskStatus.COMPLETED) {
                    if (count > 0) activeTasksList.append("\n\n"); // Jarak antar tugas

                    // Format:
                    // • Judul Tugas (Status)
                    activeTasksList.append("• ").append(t.getTitle())
                            .append(" (").append(t.getStatus()).append(")");

                    count++;
                }
            }

            // 3. Tampilkan ke Label
            if (count > 0) {
                lblTarget.setText(activeTasksList.toString());
                lblTarget.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
            } else {
                // Jika tidak ada tugas aktif, tampilkan pesan default atau target statis
                String defaultTarget = currentEmployee.getDailyTarget();
                if (defaultTarget != null && !defaultTarget.equals("-") && !defaultTarget.isEmpty()) {
                    lblTarget.setText("Target Utama: " + defaultTarget + "\n(Tidak ada tugas pending)");
                } else {
                    lblTarget.setText("Semua tugas selesai! Kerja bagus.");
                    lblTarget.setStyle("-fx-font-size: 14px; -fx-text-fill: green; -fx-font-style: italic;");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblTarget.setText("Gagal memuat tugas.");
        }
    }

    private void updateAttendanceStatusUI() {
        // Cek data hari ini ke database
        com.omnitask.model.Attendance todayAtt = sparqlService.getTodayAttendance(currentEmployee.getId());

        if (todayAtt != null) {
            // SUDAH ABSEN (Bisa PRESENT, LATE, SICK, PERMIT)
            String status = todayAtt.getStatus();
            lblResult.setText("Status Hari Ini: " + status);

            // Atur Warna Berdasarkan Status
            switch (status) {
                case "PRESENT":
                    lblResult.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 14px;");
                    btnCheckIn.setDisable(true); // Sudah absen, disable checkin
                    break;
                case "LATE":
                    lblResult.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 14px;");
                    btnCheckIn.setDisable(true);
                    break;
                case "SICK":
                case "PERMIT":
                    lblResult.setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-font-size: 14px;");
                    btnCheckIn.setDisable(true); // Sakit/Izin tidak perlu absen
                    break;
                default:
                    lblResult.setStyle("-fx-text-fill: black;");
            }

            // Cek apakah sudah checkout juga?
            if (todayAtt.getCheckOutTime() != null) {
                lblResult.setText(lblResult.getText() + "");
                btnCheckOut.setDisable(true);
            }

        } else {
            // BELUM ABSEN (ALPHA)
            lblResult.setText("Status: ALPHA (Belum Absen)");
            lblResult.setStyle("-fx-text-fill: red; -fx-font-style: italic;");
            // Tombol CheckIn akan di-handle oleh checkLocation() nanti
        }
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
            Scene currentScene = btnEmployees.getScene();
            currentScene.setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Gagal membuka halaman Employee: " + e.getMessage()).show();
        }
    }

    // 4. CEK LOKASI
    private void checkLocation() {
        // Simulasi Lokasi
        Location loc = getUserCurrentLocation();
        boolean isOffice = geofenceService.isWithinGeofence(loc);

        if (isOffice) {
            lblLocationStatus.setText("Lokasi: Kantor");
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
                Location loc = getUserCurrentLocation();
                if (isCheckIn) {
                    attendanceService.checkIn(currentEmployee.getId(), loc, "-");
                } else {
                    attendanceService.checkOut(currentEmployee.getId(), loc, "-");
                }

                Platform.runLater(() -> {
                    // Update UI Status terbaru (Warna & Teks)
                    updateAttendanceStatusUI();

                    // Tambahan info sukses
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Sukses");
                    alert.setHeaderText(null);
                    alert.setContentText(isCheckIn ? "Berhasil Check-In!" : "Berhasil Check-Out!");
                    alert.show();
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

            TaskController taskController = loader.getController();
            taskController.setEmployee(currentEmployee);

            // Pindah Scene
            Scene currentScene = btnCheckIn.getScene();
            currentScene.setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // GPS
    private Location getUserCurrentLocation() {
        // Ganti angka di sini jika ingin tes simulasi "Di Luar Kantor"
        // Default: Sama dengan kantor (3.5952, 98.6722)
        return new Location(3.5952, 98.6722);
    }


    @FXML
    private void handleGoToLeaveRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LeaveRequestView.fxml"));
            Parent root = loader.load();

            // Ganti Scene
            // (Gunakan salah satu komponen UI yang ada untuk getScene())
            Scene currentScene = btnCheckIn.getScene(); // Contoh di AttendanceController
            currentScene.setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            // Show Alert Error
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}