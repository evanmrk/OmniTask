package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.LeaveRequest;
import com.omnitask.service.SPARQLService;
import com.omnitask.service.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.UUID;

public class LeaveRequestController {

    // UI
    @FXML private TextField txtName;
    @FXML private TextField txtId;
    @FXML private ComboBox<String> cbLeaveType;

    // Form General
    @FXML private VBox formGeneral;
    @FXML private DatePicker dpStartDateGeneral;

    @FXML private TextArea txtReason;

    // Form Dinas
    @FXML private VBox formDinas;
    @FXML private TextField txtDestination;
    @FXML private TextArea txtPurpose;
    @FXML private DatePicker dpStartDateDinas;
    @FXML private DatePicker dpEndDateDinas;
    @FXML private TextField txtTransport;

    // Tombol Manager (Pastikan @FXML ini ada)
    @FXML private Button btnEmployees;
    @FXML private TableView<LeaveRequest> tableMyRequests;
    @FXML private TableColumn<LeaveRequest, String> colReqId;
    @FXML private TableColumn<LeaveRequest, String> colDate;
    @FXML private TableColumn<LeaveRequest, String> colType;
    @FXML private TableColumn<LeaveRequest, String> colStatus;
    @FXML private TableColumn<LeaveRequest, String> colReason;
    @FXML private DatePicker dpEndDateGeneral;

    private SPARQLService sparqlService;
    private Employee currentUser;

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();

        // 1. Ambil data User dari Session
        if (SessionManager.isLoggedIn()) {
            currentUser = SessionManager.getCurrentUser();
            txtName.setText(currentUser.getName());
            txtId.setText(currentUser.getId());

            // --- LOGIKA CEK ROLE MANAGER (Agar tombol muncul) ---
            String role = currentUser.getRole();
            // Cek null pada btnEmployees untuk menghindari error jika FXML belum update
            if (btnEmployees != null && role != null && role.toLowerCase().contains("manager")) {
                btnEmployees.setVisible(true);
                btnEmployees.setManaged(true);
            } else if (btnEmployees != null) {
                btnEmployees.setVisible(false);
                btnEmployees.setManaged(false);
            }
            // ----------------------------------------------------
            setupTable();
            loadMyRequests();
        }

        // 2. Isi Pilihan Izin
        cbLeaveType.setItems(FXCollections.observableArrayList("Cuti", "Izin", "Perjalanan Dinas"));

        // 3. Listener form
        cbLeaveType.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFormVisibility(newVal);
        });
    }

    private void loadMyRequests() {
        if (currentUser == null) return;

        try {
            // Ambil data izin milik user yang sedang login dari database
            java.util.List<com.omnitask.model.LeaveRequest> myList = sparqlService.getLeaveRequestsByEmployee(currentUser.getId());

            // Masukkan ke dalam tabel
            javafx.collections.ObservableList<com.omnitask.model.LeaveRequest> data = javafx.collections.FXCollections.observableArrayList(myList);
            tableMyRequests.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Gagal memuat riwayat izin: " + e.getMessage());
        }
    }

    private void setupTable() {
        // Hubungkan kolom tabel dengan data model
        colReqId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRequestId()));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().name()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Format Tanggal
        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getStartDate() != null) {
                return new SimpleStringProperty(cell.getValue().getStartDate().format(java.time.format.DateTimeFormatter.ISO_DATE));
            }
            return new SimpleStringProperty("-");
        });

        // Alasan
        colReason.setCellValueFactory(cell -> {
            String r = cell.getValue().getReason();
            return new SimpleStringProperty(r != null ? r : "-");
        });
    }

    private void updateFormVisibility(String type) {
        formGeneral.setVisible(false); formGeneral.setManaged(false);
        formDinas.setVisible(false); formDinas.setManaged(false);

        if (type == null) return;

        if (type.equals("Perjalanan Dinas")) {
            formDinas.setVisible(true); formDinas.setManaged(true);
        } else {
            formGeneral.setVisible(true); formGeneral.setManaged(true);
        }
    }

    @FXML
    private void handleSubmit() {
        String typeSelection = cbLeaveType.getValue();
        if (typeSelection == null) {
            showAlert("Error", "Pilih jenis pengajuan terlebih dahulu!");
            return;
        }

        try {
            LeaveRequest req = new LeaveRequest();
            req.setRequestId("REQ-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            req.setEmployeeId(currentUser.getId());
            req.setEmployeeName(currentUser.getName());
            req.setStatus(LeaveRequest.RequestStatus.PENDING);

            if (typeSelection.equals("Perjalanan Dinas")) {
                if (txtDestination.getText().isEmpty() || txtPurpose.getText().isEmpty() ||
                        dpStartDateDinas.getValue() == null || dpEndDateDinas.getValue() == null ||
                        txtTransport.getText().isEmpty()) {
                    showAlert("Validasi Gagal", "Semua kolom Dinas wajib diisi!");
                    return;
                }
                req.setType(LeaveRequest.RequestType.DINAS);
                req.setDestination(txtDestination.getText());
                req.setReason(txtPurpose.getText());
                req.setStartDate(dpStartDateDinas.getValue());
                req.setEndDate(dpEndDateDinas.getValue());
                req.setTransport(txtTransport.getText());

            } else {
                if (dpStartDateGeneral.getValue() == null || dpEndDateGeneral.getValue() == null ||
                        txtReason.getText().isEmpty()) {
                    showAlert("Validasi Gagal", "Semua kolom Cuti/Izin wajib diisi!");
                    return;
                }

                // Validasi Tanggal
                if (dpEndDateGeneral.getValue().isBefore(dpStartDateGeneral.getValue())) {
                    showAlert("Validasi Gagal", "Tanggal Selesai tidak boleh sebelum Tanggal Mulai.");
                    return;
                }

                req.setType(typeSelection.equals("Cuti") ? LeaveRequest.RequestType.CUTI : LeaveRequest.RequestType.IZIN);
                req.setStartDate(dpStartDateGeneral.getValue());
                req.setEndDate(dpEndDateGeneral.getValue());

                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                        dpStartDateGeneral.getValue(), dpEndDateGeneral.getValue()
                ) + 1;

                req.setDuration(daysBetween + " Hari"); // Simpan hasil hitungan ke database

                req.setReason(txtReason.getText());
            }
            sparqlService.saveLeaveRequest(req);
            showAlert("Berhasil", "Pengajuan berhasil dikirim!\nID: " + req.getRequestId() + "\nStatus: MENUNGGU PERSETUJUAN");
            handleBackToDashboard();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menyimpan: " + e.getMessage());
        }
    }

    // --- NAVIGASI ---

    @FXML
    private void handleBackToDashboard() {
        navigateTo("/fxml/attendance_view.fxml");
    }

    @FXML
    private void handleGoToTasks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskPage.fxml"));
            Parent root = loader.load();

            // FIX: Kita wajib mengirim data Employee ke TaskController
            // agar logika pengecekan Manager di TaskController berjalan dan tombol muncul.
            TaskController taskController = loader.getController();
            taskController.setEmployee(SessionManager.getCurrentUser());

            Scene currentScene = txtName.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal ke Tasks: " + e.getMessage());
        }
    }

    // Navigasi ke Employee ---
    @FXML
    private void handleGoToEmployees() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EmployeePage.fxml"));
            Parent root = loader.load();

            // PERBAIKAN: Gunakan txtName (TextField Nama) untuk mengambil Scene
            // Karena btnEmployees mungkin null atau bermasalah, txtName lebih aman
            Scene currentScene = txtName.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman Employee: " + e.getMessage());
        }
    }
    // -----------------------------------------------------------

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Gunakan txtName juga di sini agar konsisten
            Scene currentScene = txtName.getScene();
            currentScene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleDelete() {
        // 1. Ambil data yang dipilih di tabel riwayat
        if (tableMyRequests == null) return;

        com.omnitask.model.LeaveRequest selected = tableMyRequests.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Pilih Data", "Silakan pilih riwayat izin di tabel yang ingin dihapus.");
            return;
        }

        // 2. Validasi: Hanya boleh hapus jika status masih PENDING
        if (selected.getStatus() != com.omnitask.model.LeaveRequest.RequestStatus.PENDING) {
            showAlert("Gagal", "Hanya pengajuan berstatus PENDING yang boleh dibatalkan/dihapus.");
            return;
        }

        // 3. Konfirmasi Hapus
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Batalkan Pengajuan");
        confirm.setHeaderText("Batalkan izin ID: " + selected.getRequestId() + "?");
        confirm.setContentText("Data akan dihapus permanen dari sistem.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                // Panggil Service Delete (Pastikan method ini ada di SPARQLService)
                sparqlService.deleteLeaveRequest(selected.getRequestId());

                // Refresh Tabel
                handleRefresh();

                showAlert("Sukses", "Pengajuan berhasil dibatalkan.");

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal menghapus: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRefresh() {
        // Panggil fungsi untuk memuat ulang data tabel
        loadMyRequests();
    }

}