package com.omnitask.controller;

import com.omnitask.model.Attendance;
import com.omnitask.model.Employee;
import com.omnitask.model.Task;
import com.omnitask.service.SPARQLService;
import com.omnitask.service.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EmployeeController {

    @FXML private Label lblManagerName;
    @FXML private TableView<Employee> tableEmployee;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colTarget;

    private SPARQLService sparqlService;
    private Employee currentManager;

    @FXML
    public void initialize() {
        // --- PERBAIKAN UTAMA DI SINI ---
        // Kita harus membuat instance service dulu sebelum dipakai!
        sparqlService = new SPARQLService();

        if (SessionManager.isLoggedIn()) {
            this.currentManager = SessionManager.getCurrentUser();

            // Set Label Nama di Pojok Kanan Atas
            if (lblManagerName != null) {
                lblManagerName.setText(currentManager.getName());
            }
        }

        // Setup Kolom Tabel
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole()));

        // Tambahan: Setup kolom Target agar datanya muncul (karena variabelnya ada di FXML)
        if (colTarget != null) {
            colTarget.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDailyTarget()));
        }

        loadData();
    }

    private void loadData() {
        try {
            List<Employee> employees = sparqlService.getAllEmployees();
            ObservableList<Employee> data = FXCollections.observableArrayList(employees);
            tableEmployee.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Gagal load data: " + e.getMessage());
        }
    }

    public void setManager(Employee manager) {
        this.currentManager = manager;
        if (lblManagerName != null) {
            lblManagerName.setText(manager.getName());
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    // ==========================================
    // FITUR 1: BUAT TASK (POPUP) - (Assign Task Lama/Simple)
    // ==========================================
    @FXML
    private void handleCreateTask() {
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();
        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Silakan klik salah satu karyawan di tabel dulu!");
            return;
        }

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Assign Task");
        dialog.setHeaderText("Buat Tugas untuk: " + selectedEmp.getName());

        ButtonType loginButtonType = new ButtonType("Simpan Tugas", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Judul Tugas");
        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Deskripsi detail...");
        txtDesc.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(LocalDate.now());

        grid.add(new Label("Judul:"), 0, 0);
        grid.add(txtTitle, 1, 0);
        grid.add(new Label("Deskripsi:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("Deadline:"), 0, 2);
        grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Task t = new Task();
                t.setId(UUID.randomUUID().toString().substring(0, 8));
                t.setEmployeeId(selectedEmp.getId());
                t.setTitle(txtTitle.getText());
                t.setDescription(txtDesc.getText());
                t.setDueDate(datePicker.getValue());
                t.setStatus(Task.TaskStatus.PENDING);
                t.setProgressPercentage(0);
                return t;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();

        result.ifPresent(task -> {
            sparqlService.saveTask(task);
            showAlert("Sukses", "Tugas berhasil diberikan kepada " + selectedEmp.getName());
        });
    }

    // ==========================================
    // FITUR 2: EDIT ABSENSI (REAL DATABASE UPDATE)
    // ==========================================
    @FXML
    private void handleEditAttendance() {
        // 1. Ambil karyawan yang dipilih
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();
        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Pilih karyawan yang mau diedit absensinya!");
            return;
        }

        // 2. Cek status saat ini (untuk default value di dialog)
        Attendance att = sparqlService.getTodayAttendance(selectedEmp.getId());
        String currentStatus = (att != null) ? att.getStatus() : "ALPHA"; // Default jika belum absen

        // 3. Tampilkan Dialog
        TextInputDialog td = new TextInputDialog(currentStatus);
        td.setTitle("Edit Status Absensi");
        td.setHeaderText("Set status absensi hari ini untuk: " + selectedEmp.getName());
        td.setContentText("Masukkan Status (PRESENT / SICK / LEAVE / ALPHA):");

        Optional<String> result = td.showAndWait();

        // 4. Proses Simpan
        result.ifPresent(newStatus -> {
            try {
                // Panggil Service baru yang sudah kita buat
                sparqlService.setAttendanceStatus(selectedEmp.getId(), newStatus.toUpperCase());

                showAlert("Sukses", "Status absensi " + selectedEmp.getName() +
                        " berhasil diubah menjadi: " + newStatus.toUpperCase());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal update absensi: " + e.getMessage());
            }
        });
    }

    // --- FITUR BARU: HAPUS KARYAWAN ---
    @FXML
    private void handleDeleteEmployee() {
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();

        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Silakan klik nama karyawan yang ingin dihapus.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus akun: " + selectedEmp.getName() + "?");
        alert.setContentText("Tindakan ini tidak dapat dibatalkan. ID: " + selectedEmp.getId());

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                sparqlService.deleteEmployee(selectedEmp.getId());
                showAlert("Sukses", "Akun karyawan berhasil dihapus.");
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal menghapus data: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // NAVIGASI
    // ==========================================
    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attendance_view.fxml"));
            Parent root = loader.load();

            Scene currentScene = tableEmployee.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Gagal kembali ke Dashboard: " + e.getMessage()).show();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- REGISTRASI ---
    @FXML
    private void handleRegisterEmployee() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterView.fxml"));
            Parent root = loader.load();

            Stage registerStage = new Stage();
            registerStage.setTitle("Registrasi Karyawan Baru");
            registerStage.setScene(new Scene(root));
            registerStage.initModality(Modality.APPLICATION_MODAL);
            registerStage.showAndWait();

            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka form registrasi: " + e.getMessage());
        }
    }

    // --- TASK MANAGER (POPUP LENGKAP) ---
    @FXML
    private void handleManageTasks() {
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();

        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Silakan klik salah satu nama karyawan di tabel terlebih dahulu.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManagerTaskView.fxml"));
            Parent root = loader.load();

            ManagerTaskController taskController = loader.getController();
            taskController.setTargetEmployee(selectedEmp);

            Stage stage = new Stage();
            stage.setTitle("Task Manager - " + selectedEmp.getName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka task manager: " + e.getMessage());
        }
    }

    // --- NAVIGASI KE HALAMAN TASK PRIBADI ---
    @FXML
    private void handleGoToTasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskPage.fxml"));
            Parent root = loader.load();

            TaskController taskController = loader.getController();
            // Ambil dari Session agar aman (tidak null)
            taskController.setEmployee(SessionManager.getCurrentUser());

            Scene currentScene = tableEmployee.getScene();
            currentScene.setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Gagal ke Tasks: " + e.getMessage()).show();
        }
    }
}