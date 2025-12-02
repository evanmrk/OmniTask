package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.Task;
import com.omnitask.service.SPARQLService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ManagerTaskController {

    @FXML private Label lblEmployeeName;
    @FXML private TableView<Task> tableTasks;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colDesc;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, Integer> colProgress;

    private SPARQLService sparqlService;
    private Employee targetEmployee; // Karyawan yang sedang diedit

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();

        // Mapping Kolom Tabel
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colDesc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name())); // Enum ke String
        colProgress.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getProgressPercentage()).asObject());
    }

    // Dipanggil dari EmployeeController untuk set target
    public void setTargetEmployee(Employee employee) {
        this.targetEmployee = employee;
        lblEmployeeName.setText("Mengelola tugas untuk: " + employee.getName());
        loadTasks();
    }

    @FXML
    private void handleRefresh() {
        loadTasks();
    }

    private void loadTasks() {
        if (targetEmployee == null) return;

        // Ambil task khusus karyawan ini saja
        List<Task> tasks = sparqlService.getTasksForEmployee(targetEmployee.getId());
        ObservableList<Task> data = FXCollections.observableArrayList(tasks);
        tableTasks.setItems(data);
    }

    // --- FITUR TAMBAH TASK ---
    @FXML
    private void handleAddTask() {
        // Dialog Form Sederhana
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Tambah Tugas Baru");
        dialog.setHeaderText("Berikan tugas kepada " + targetEmployee.getName());

        ButtonType saveButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField txtTitle = new TextField(); txtTitle.setPromptText("Judul Tugas");
        TextArea txtDesc = new TextArea(); txtDesc.setPromptText("Deskripsi"); txtDesc.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(LocalDate.now());

        grid.add(new Label("Judul:"), 0, 0); grid.add(txtTitle, 1, 0);
        grid.add(new Label("Deskripsi:"), 0, 1); grid.add(txtDesc, 1, 1);
        grid.add(new Label("Deadline:"), 0, 2); grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Task t = new Task();
                t.setId(UUID.randomUUID().toString().substring(0, 8));
                t.setEmployeeId(targetEmployee.getId()); // Link ke karyawan ini
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
            loadTasks(); // Refresh tabel setelah simpan
        });
    }

    // --- FITUR HAPUS TASK ---
    @FXML
    private void handleDeleteTask() {
        Task selected = tableTasks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Pilih tugas yang mau dihapus dulu!").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Yakin hapus tugas: " + selected.getTitle() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sparqlService.deleteTask(selected.getId());
                loadTasks(); // Refresh tabel setelah hapus
            }
        });
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblEmployeeName.getScene().getWindow();
        stage.close();
    }
}