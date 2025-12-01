package com.omnitask.controller;

import com.omnitask.model.Task;
import com.omnitask.service.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class TaskOverviewController {

    // --- UI Components (Linked to FXML) ---
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, LocalDate> colDueDate;
    @FXML private TableColumn<Task, Task.TaskStatus> colStatus;

    // Detail Section
    @FXML private Label lblTitle;
    @FXML private TextArea txtDescription;
    @FXML private Label lblDueDate;
    @FXML private Label lblStatus;
    @FXML private Slider sliderProgress;
    @FXML private Label lblProgressPercent;
    @FXML private TextArea txtDailyNotes;
    @FXML private Button btnUpdateProgress;
    @FXML private Button btnAddNote;

    // --- Data & Service ---
    private TaskService taskService;
    private ObservableList<Task> taskList;
    private Task currentSelectedTask;

    // TODO: Nanti ini didapat dari Class 'UserSession' saat Login
    private final String CURRENT_EMPLOYEE_ID = "EMP-001";

    public TaskOverviewController() {
        this.taskService = new TaskService();
    }

    @FXML
    public void initialize() {
        // 1. Setup Kolom Tabel
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Load Data Awal
        loadTasks();

        // 3. Listener saat baris tabel dipilih
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTaskDetails(newValue)
        );

        // 4. Listener untuk Slider Progress (Agar Label % berubah realtime saat digeser)
        sliderProgress.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblProgressPercent.setText(String.format("%.0f%%", newVal));
        });
    }

    private void loadTasks() {
        // Mengambil data dari Service
        List<Task> tasks = taskService.getTasksForEmployee(CURRENT_EMPLOYEE_ID);
        taskList = FXCollections.observableArrayList(tasks);
        taskTable.setItems(taskList);
    }

    private void showTaskDetails(Task task) {
        currentSelectedTask = task;

        if (task != null) {
            // Tampilkan data ke UI sebelah kanan
            lblTitle.setText(task.getTitle());
            txtDescription.setText(task.getDescription());
            lblDueDate.setText(task.getDueDate().toString());
            lblStatus.setText(task.getStatus().toString());

            // Set Progress
            sliderProgress.setValue(task.getProgressPercentage());
            lblProgressPercent.setText(task.getProgressPercentage() + "%");

            // Set Notes (ReadOnly di box utama, input baru di dialog terpisah atau append)
            txtDailyNotes.setText(task.getDailyNotes() != null ? task.getDailyNotes() : "Belum ada catatan.");

            // Enable tombol
            btnUpdateProgress.setDisable(false);
            btnAddNote.setDisable(false);
        } else {
            // Reset jika tidak ada yang dipilih
            lblTitle.setText("");
            txtDescription.setText("");
            btnUpdateProgress.setDisable(true);
            btnAddNote.setDisable(true);
        }
    }

    // --- User Actions (Logika Karyawan) ---

    @FXML
    private void handleUpdateProgress() {
        if (currentSelectedTask == null) return;

        try {
            int newProgress = (int) sliderProgress.getValue();

            // Panggil Service
            taskService.updateProgress(currentSelectedTask.getId(), newProgress);

            // Refresh tampilan (Karena status mungkin berubah jadi COMPLETED otomatis)
            loadTasks();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Progress berhasil diperbarui!");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal update progress: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddNote() {
        if (currentSelectedTask == null) return;

        // Contoh sederhana menggunakan TextInputDialog bawaan JavaFX
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tambah Catatan Harian");
        dialog.setHeaderText("Apa yang Anda kerjakan hari ini?");
        dialog.setContentText("Catatan:");

        dialog.showAndWait().ifPresent(note -> {
            try {
                taskService.addDailyNotes(currentSelectedTask.getId(), note);
                showTaskDetails(currentSelectedTask); // Refresh text area notes
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan catatan.");
            }
        });
    }

    // Helper untuk menampilkan pesan pop-up
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}