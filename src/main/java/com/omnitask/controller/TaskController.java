package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.Task;
import com.omnitask.model.Task.TaskStatus;
import com.omnitask.service.SPARQLService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class TaskController {

    @FXML private Label lblName;
    @FXML private VBox taskContainer;

    private SPARQLService sparqlService;
    private Employee currentEmployee;

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();
    }

    /**
     * Method ini WAJIB dipanggil dari AttendanceController setelah login
     * untuk mengoper data Employee yang sedang aktif.
     */
    public void setEmployee(Employee employee) {
        this.currentEmployee = employee;
        lblName.setText(employee.getName());
        loadDailyTasks();
    }

    @FXML
    private void handleRefresh() {
        if (currentEmployee != null) {
            loadDailyTasks();
        }
    }

    private void loadDailyTasks() {
        taskContainer.getChildren().clear();

        // Menggunakan method getTodayTasks dari SPARQLService Anda
        List<Task> tasks = sparqlService.getTodayTasks(currentEmployee.getId());

        if (tasks.isEmpty()) {
            Label emptyLbl = new Label("Tidak ada tugas terjadwal untuk hari ini.");
            emptyLbl.setStyle("-fx-text-fill: grey; -fx-font-style: italic;");
            taskContainer.getChildren().add(emptyLbl);
        } else {
            for (Task task : tasks) {
                VBox card = createTaskCard(task);
                taskContainer.getChildren().add(card);
            }
        }
    }

    private VBox createTaskCard(Task task) {
        // 1. Container Kartu
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

        // PENTING: Simpan objek Task asli di dalam UserData node agar mudah diambil saat Save
        card.setUserData(task);

        // 2. Judul & Deskripsi
        Label lblTitle = new Label(task.getTitle());
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label lblDesc = new Label(task.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #555;");

        // 3. Opsi Radio Button
        ToggleGroup group = new ToggleGroup();
        RadioButton rbSelesai = new RadioButton("Selesai");
        RadioButton rbOnGoing = new RadioButton("On Going");
        RadioButton rbTidak = new RadioButton("Tidak");

        rbSelesai.setToggleGroup(group);
        rbOnGoing.setToggleGroup(group);
        rbTidak.setToggleGroup(group);

        // Set status awal berdasarkan data database
        if (task.getStatus() == TaskStatus.COMPLETED) {
            rbSelesai.setSelected(true);
        }
        else if (task.getStatus() == TaskStatus.IN_PROGRESS) { // Pastikan IN_PROGRESS sesuai dengan Enum Anda
            rbOnGoing.setSelected(true);
        }
        else {
            // Mencakup PENDING, atau status apapun selain dua di atas (termasuk jika BLOCKED tidak ada)
            rbTidak.setSelected(true);
        }

        HBox optionsBox = new HBox(20, rbSelesai, rbOnGoing, rbTidak);

        // 4. Input Alasan / Notes (Hidden by default)
        TextField txtReason = new TextField();
        txtReason.setPromptText("Masukkan alasan / kendala...");
        txtReason.setPrefWidth(300);

        // Isi notes jika sudah ada sebelumnya
        if (task.getDailyNotes() != null) {
            txtReason.setText(task.getDailyNotes());
        }

        // Logic Visibility Text Field
        // Default hidden kecuali statusnya memang 'pending/blocked' dan user pilih 'Tidak'
        boolean showReason = rbTidak.isSelected();
        txtReason.setVisible(showReason);
        txtReason.setManaged(showReason);

        // Listener perubahan Radio Button
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isTidak = (newVal == rbTidak);
            txtReason.setVisible(isTidak);
            txtReason.setManaged(isTidak);
            if (!isTidak) txtReason.clear();
        });

        card.getChildren().addAll(lblTitle, lblDesc, new Separator(), optionsBox, txtReason);
        return card;
    }

    @FXML
    private void handleSaveAll() {
        int successCount = 0;

        for (Node node : taskContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                Task task = (Task) card.getUserData(); // Ambil object Task asli

                // Ambil komponen dari UI
                // Index anak-anak VBox: 0=Title, 1=Desc, 2=Separator, 3=HBox(Radio), 4=TextField
                HBox optionsBox = (HBox) card.getChildren().get(3);
                TextField txtReason = (TextField) card.getChildren().get(4);

                String selectedText = "";
                RadioButton selectedRb = (RadioButton) ((ToggleGroup) ((RadioButton)optionsBox.getChildren().get(0)).getToggleGroup()).getSelectedToggle();

                if (selectedRb != null) {
                    selectedText = selectedRb.getText();
                }

                // Update Object Task berdasarkan input UI
                if (selectedText.equals("Selesai")) {
                    task.setStatus(TaskStatus.COMPLETED);
                    task.setProgressPercentage(100);
                    task.setDailyNotes(""); // Clear notes jika sukses
                } else if (selectedText.equals("On Going")) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    task.setProgressPercentage(50);
                    task.setDailyNotes("");
                } else if (selectedText.equals("Tidak")) {
                    // Cek validasi
                    if (txtReason.getText().trim().isEmpty()) {
                        showAlert("Peringatan", "Alasan wajib diisi untuk tugas: " + task.getTitle());
                        return; // Stop proses save
                    }
                    task.setStatus(TaskStatus.PENDING); // Atau status lain spt FAILED/BLOCKED
                    task.setProgressPercentage(0);
                    task.setDailyNotes(txtReason.getText());
                }

                // Panggil Service Update Task Anda
                sparqlService.updateTask(task);
                successCount++;
            }
        }

        if (successCount > 0) {
            showAlert("Sukses", "Berhasil memperbarui " + successCount + " tugas ke database.");
        }
    }

    // --- NAVIGASI ---

    @FXML
    private void handleBackToAttendance() {
        try {
            // PERBAIKAN: Sesuaikan dengan nama file di screenshot Anda
            // Folder: /fxml/
            // File: attendance_view.fxml
            java.net.URL fxmlLocation = getClass().getResource("/fxml/attendance_view.fxml");

            if (fxmlLocation == null) {
                System.err.println("ERROR: File tidak ditemukan di /fxml/attendance_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            // Pindah Scene
            Stage stage = (Stage) taskContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Tampilkan alert jika gagal
            new Alert(Alert.AlertType.ERROR, "Gagal kembali: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleLogout() {
        // Logic logout, kembali ke Attendance Page state awal
        handleBackToAttendance();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}