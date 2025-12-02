package com.omnitask.controller;

import com.omnitask.model.Attendance;
import com.omnitask.model.Employee;
import com.omnitask.model.Task;
import com.omnitask.service.SPARQLService;
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
    @FXML private TableColumn<Employee, String> colDept;
    @FXML private TableColumn<Employee, String> colRole;
    @FXML private TableColumn<Employee, String> colTarget;

    private SPARQLService sparqlService;
    private Employee currentManager; // Data manajer yang sedang login




    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();

        // Mapping ini mencocokkan kolom Tabel dengan Method Getter di Employee.java

        // Col ID -> getId()
        colId.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getId()));

        // Col Name -> getName()
        colName.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));

        // Col Dept -> getDepartment()
        colDept.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDepartment()));

        // Col Role -> getRole()
        colRole.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getRole()));

        // Col Target -> getDailyTarget() (Hanya jika kolom ini ada di FXML)
        if (colTarget != null) {
            colTarget.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDailyTarget()));
        }

        loadData();
    }

    private void loadData() {
        java.util.List<Employee> employees = sparqlService.getAllEmployees();
        javafx.collections.ObservableList<Employee> data = javafx.collections.FXCollections.observableArrayList(employees);
        tableEmployee.setItems(data);
    }

    public void setManager(Employee manager) {
        this.currentManager = manager;
        lblManagerName.setText("Manager: " + manager.getName());
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }


    // ==========================================
    // FITUR 1: BUAT TASK (POPUP)
    // ==========================================
    @FXML
    private void handleCreateTask() {
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();
        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Silakan klik salah satu karyawan di tabel dulu!");
            return;
        }

        // Buat Dialog Input Custom
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

        // Konversi hasil input menjadi Objek Task
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Task t = new Task();
                t.setId(UUID.randomUUID().toString().substring(0, 8)); // Generate random ID
                t.setEmployeeId(selectedEmp.getId());
                t.setTitle(txtTitle.getText());
                t.setDescription(txtDesc.getText());
                t.setDueDate(datePicker.getValue());
                t.setStatus(Task.TaskStatus.PENDING); // Default status
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
    // FITUR 2: EDIT ABSENSI (POPUP)
    // ==========================================
    @FXML
    private void handleEditAttendance() {
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();
        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Pilih karyawan yang mau diedit absensinya!");
            return;
        }

        // Di sini idealnya kita load dulu absensi hari ini
        Attendance att = sparqlService.getTodayAttendance(selectedEmp.getId());

        // Form Input Sederhana
        TextInputDialog td = new TextInputDialog(att != null ? att.getStatus() : "PRESENT");
        td.setTitle("Edit Status Absensi");
        td.setHeaderText("Ubah status absensi hari ini untuk: " + selectedEmp.getName());
        td.setContentText("Masukkan Status (PRESENT / SICK / LEAVE / ALPHA):");

        Optional<String> result = td.showAndWait();
        result.ifPresent(newStatus -> {
            // Logic update ke database (SPARQL Update manual string)
            // Anda mungkin perlu membuat method updateAttendanceStatus di SPARQLService
            // Untuk demo ini kita simulasi print saja
            System.out.println("Mengupdate status " + selectedEmp.getName() + " menjadi " + newStatus);
            showAlert("Info", "Fitur Edit Absensi (Simulasi): Status diubah ke " + newStatus);
        });
    }

    // ==========================================
    // NAVIGASI
    // ==========================================
    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attendance_view.fxml"));
            Parent root = loader.load();

            AttendanceController attController = loader.getController();
            attController.restoreSession(this.currentManager); // Bawa data manager balik

//            Stage stage = (Stage) tableEmployee.getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();

            Scene currentScene = tableEmployee.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }


    // Registrasi

    @FXML
    private void handleRegisterEmployee() {
        try {
            // 1. Load FXML RegisterView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterView.fxml")); // Pastikan path benar
            Parent root = loader.load();

            // 2. Buat Stage Baru (Jendela Pop-up)
            Stage registerStage = new Stage();
            registerStage.setTitle("Registrasi Karyawan Baru");
            registerStage.setScene(new Scene(root));

            // 3. Set Modality: Agar user TIDAK BISA klik window belakang sebelum ini ditutup
            registerStage.initModality(Modality.APPLICATION_MODAL);

            // 4. Tampilkan dan Tunggu sampai ditutup
            registerStage.showAndWait();

            // 5. Setelah ditutup, otomatis Refresh Tabel agar data baru muncul
            loadData();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka form registrasi: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageTasks() {
        // 1. Cek apakah ada karyawan yang dipilih di tabel
        Employee selectedEmp = tableEmployee.getSelectionModel().getSelectedItem();

        if (selectedEmp == null) {
            showAlert("Pilih Karyawan", "Silakan klik salah satu nama karyawan di tabel terlebih dahulu.");
            return;
        }

        try {
            // 2. Load View ManagerTaskView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManagerTaskView.fxml"));
            Parent root = loader.load();

            // 3. Ambil Controller-nya dan kirim data Karyawan yang dipilih
            ManagerTaskController taskController = loader.getController();
            taskController.setTargetEmployee(selectedEmp); // <--- INI PENTING

            // 4. Tampilkan sebagai Pop-up (Modal)
            Stage stage = new Stage();
            stage.setTitle("Task Manager - " + selectedEmp.getName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Window belakang tidak bisa diklik
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka task manager: " + e.getMessage());
        }
    }
}