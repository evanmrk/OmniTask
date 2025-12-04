package com.omnitask.controller;

import com.omnitask.model.Employee;
import com.omnitask.model.LeaveRequest;
import com.omnitask.service.SPARQLService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ManagerLeaveController {

    @FXML private Label lblEmployeeName;
    @FXML private ComboBox<String> cbFilter;

    // Tabel
    @FXML private TableView<LeaveRequest> tableRequests;
    @FXML private TableColumn<LeaveRequest, String> colDate;
    @FXML private TableColumn<LeaveRequest, String> colType;
    @FXML private TableColumn<LeaveRequest, String> colStatus;

    // Detail
    @FXML private Label lblReqId, lblType, lblDateRange, lblStatusDetail, lblReason, lblDest, lblTransport;
    @FXML private VBox boxDinasDetails;
    @FXML private HBox boxActions;
    @FXML private Label lblActionInfo;

    private SPARQLService sparqlService;
    private Employee targetEmployee;
    private ObservableList<LeaveRequest> masterData = FXCollections.observableArrayList();
    private FilteredList<LeaveRequest> filteredData;

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();

        // 1. Setup Tabel
        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getStartDate() != null) {
                return new SimpleStringProperty(cell.getValue().getStartDate().toString());
            } else {
                return new SimpleStringProperty("-");
            }
        });
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().name()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Listener Klik Tabel
        tableRequests.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showDetails(newVal));

        // 2. Setup Filter
        cbFilter.setItems(FXCollections.observableArrayList(
                "Semua Izin", "Izin Aktif", "Izin Selesai", "Diterima", "Ditolak"));
        cbFilter.setValue("Semua Izin");

        cbFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    public void setTargetEmployee(Employee employee) {
        this.targetEmployee = employee;
        lblEmployeeName.setText("Karyawan: " + employee.getName() + " (" + employee.getId() + ")");
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        if (targetEmployee == null) return;
        masterData.setAll(sparqlService.getLeaveRequestsByEmployee(targetEmployee.getId()));

        // Bungkus data ke FilteredList
        filteredData = new FilteredList<>(masterData, p -> true);
        tableRequests.setItems(filteredData);

        // Re-apply filter saat ini
        applyFilter(cbFilter.getValue());
    }

    private void applyFilter(String filterType) {
        if (filteredData == null) return;

        filteredData.setPredicate(req -> {
            if (filterType == null || filterType.equals("Semua Izin")) return true;

            switch (filterType) {
                case "Izin Aktif": // PENDING
                    return req.getStatus() == LeaveRequest.RequestStatus.PENDING;
                case "Izin Selesai": // APPROVED / REJECTED
                    return req.getStatus() != LeaveRequest.RequestStatus.PENDING;
                case "Diterima":
                    return req.getStatus() == LeaveRequest.RequestStatus.APPROVED;
                case "Ditolak":
                    return req.getStatus() == LeaveRequest.RequestStatus.REJECTED;
                default:
                    return true;
            }
        });
    }

    private void showDetails(com.omnitask.model.LeaveRequest req) {
        if (req == null) {
            lblReqId.setText("-");
            lblType.setText("-");
            boxActions.setVisible(false);
            return;
        }

        // ... (Kode set text label lainnya TETAP SAMA seperti sebelumnya) ...
        lblReqId.setText(req.getRequestId());
        lblType.setText(req.getType().name());
        lblStatusDetail.setText(req.getStatus().name());

        // Warna Status
        if (req.getStatus() == com.omnitask.model.LeaveRequest.RequestStatus.APPROVED)
            lblStatusDetail.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        else if (req.getStatus() == com.omnitask.model.LeaveRequest.RequestStatus.REJECTED)
            lblStatusDetail.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        else
            lblStatusDetail.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");

        // Tanggal & Alasan
        String range = (req.getStartDate() != null) ? req.getStartDate().toString() : "-";
        if (req.getEndDate() != null) range += " s/d " + req.getEndDate().toString();
        else if (req.getDuration() != null) range += " (" + req.getDuration() + ")";
        lblDateRange.setText(range);

        lblReason.setText(req.getReason() != null ? req.getReason() : "-");

        // Info Dinas
        if (req.getType() == com.omnitask.model.LeaveRequest.RequestType.DINAS) {
            boxDinasDetails.setVisible(true); boxDinasDetails.setManaged(true);
            lblDest.setText(req.getDestination());
            lblTransport.setText(req.getTransport());
        } else {
            boxDinasDetails.setVisible(false); boxDinasDetails.setManaged(false);
        }

        // --- PERBAIKAN VISIBILITY TOMBOL (INDEX YANG BENAR) ---

        // 1. Kotak Aksi Selalu Muncul (untuk tombol Hapus)
        boxActions.setVisible(true);

        // 2. Cek apakah status PENDING?
        boolean isPending = (req.getStatus() == com.omnitask.model.LeaveRequest.RequestStatus.PENDING);

        // Urutan di FXML: [0]Label, [1]Hapus, [2]Reject, [3]Accept
        if (boxActions.getChildren().size() >= 4) {
            // Tombol Hapus (Index 1) -> Selalu Muncul
            boxActions.getChildren().get(1).setVisible(true);
            boxActions.getChildren().get(1).setManaged(true);

            // Tombol Reject (Index 2) -> Hanya jika Pending
            boxActions.getChildren().get(2).setVisible(isPending);
            boxActions.getChildren().get(2).setManaged(isPending);

            // Tombol Accept (Index 3) -> Hanya jika Pending
            boxActions.getChildren().get(3).setVisible(isPending);
            boxActions.getChildren().get(3).setManaged(isPending);
        }

        lblActionInfo.setVisible(!isPending);
    }


    @FXML
    private void handleAccept() {
        processAction(LeaveRequest.RequestStatus.APPROVED);
    }

    @FXML
    private void handleReject() {
        processAction(LeaveRequest.RequestStatus.REJECTED);
    }

    private void processAction(com.omnitask.model.LeaveRequest.RequestStatus newStatus) {
        com.omnitask.model.LeaveRequest selected = tableRequests.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Validasi: Jangan izinkan ubah jika status bukan PENDING
        if (selected.getStatus() != com.omnitask.model.LeaveRequest.RequestStatus.PENDING) {
            new Alert(Alert.AlertType.WARNING, "Permohonan ini sudah diproses! Tidak bisa diubah lagi.").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Ubah status menjadi " + newStatus.name() + "?");
        confirm.setContentText("Aksi ini tidak dapat dibatalkan.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            sparqlService.updateLeaveRequestStatus(selected.getRequestId(), newStatus.name());
            loadData();
            showDetails(null);
            new Alert(Alert.AlertType.INFORMATION, "Status diperbarui!").show();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblEmployeeName.getScene().getWindow();
        stage.close();
    }

    // Hapus Izin
    @FXML
    private void handleDelete() {
        // 1. Ambil data dari tabel
        com.omnitask.model.LeaveRequest selected = tableRequests.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Pilih data yang ingin dihapus!").show();
            return;
        }

        // 2. Konfirmasi
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hapus Data");
        confirm.setHeaderText("Hapus Permohonan ID: " + selected.getRequestId() + "?");
        confirm.setContentText("Data akan dihapus permanen dari database.");

        java.util.Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // 3. Panggil Service (Pastikan method ini ada di SPARQLService)
                sparqlService.deleteLeaveRequest(selected.getRequestId());

                // 4. Refresh Tabel
                loadData();

                // 5. Bersihkan Detail
                showDetails(null);

                new Alert(Alert.AlertType.INFORMATION, "Berhasil dihapus.").show();

            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Gagal: " + e.getMessage()).show();
            }
        }
    }


}