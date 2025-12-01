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
import javafx.stage.Stage;

import java.time.LocalDate;
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

        // Setup Kolom Tabel agar bisa baca data object Employee
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colDept.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDepartment()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole()));
        colTarget.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDailyTarget()));
        loadData();
    }

    public void setManager(Employee manager) {
        this.currentManager = manager;
        lblManagerName.setText("Manager: " + manager.getName());
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        ObservableList<Employee> data = FXCollections.observableArrayList(sparqlService.getAllEmployees());
        tableEmployee.setItems(data);
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

            Stage stage = (Stage) tableEmployee.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
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
}