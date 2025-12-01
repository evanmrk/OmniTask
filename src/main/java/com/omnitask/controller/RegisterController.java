package com.omnitask.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.omnitask.model.Employee;
import com.omnitask.service.SPARQLService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private ImageView imgPreview;
    @FXML private Label lblStatus;

    private File selectedFile;
    private SPARQLService sparqlService;

    @FXML
    public void initialize() {
        sparqlService = new SPARQLService();
        
        // --- LOGIKA AUTO GENERATE ID ---
        generateNewId();
    }

    // Method khusus untuk membuat ID baru
    private void generateNewId() {
        String uniqueCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String autoId = "EMP-" + uniqueCode; 
        txtId.setText(autoId);
    }

    // --- BAGIAN BARU: NAVIGASI KEMBALI KE LOGIN ---
    @FXML
    private void handleBackToLogin() {
        try {
            // Load Halaman Login (Attendance View)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attendance_view.fxml"));
            Parent loginRoot = loader.load();
            
            // Ambil Stage saat ini
            Stage stage = (Stage) txtId.getScene().getWindow();
            
            // Kembalikan ukuran window ke ukuran Dashboard (1000x700)
            stage.setScene(new Scene(loginRoot, 1000, 700));
            stage.setTitle("OmniTask - Employee Attendance System");
            
        } catch (IOException e) {
            e.printStackTrace();
            lblStatus.setText("Gagal kembali ke login.");
        }
    }

    private String getProjectPhotoFolder() {
        String projectPath = System.getProperty("user.dir");
        String folderPath = projectPath + File.separator + "user_photos" + File.separator;
        File folder = new File(folderPath);
        if (!folder.exists()) folder.mkdirs();
        return folderPath;
    }

    @FXML
    private void handleBrowse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Foto Profil");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg")
        );
        selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            imgPreview.setImage(new Image(selectedFile.toURI().toString()));
            lblStatus.setText("Foto dipilih.");
        }
    }

    @FXML
    private void handleSave() {
        // Validasi
        if (txtName.getText().isEmpty() || selectedFile == null) {
            lblStatus.setText("Error: Nama dan Foto wajib diisi!");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            String id = txtId.getText().trim(); 
            String name = txtName.getText().trim();
            
            // 1. COPY FOTO
            String originalName = selectedFile.getName();
            String ext = "";
            int i = originalName.lastIndexOf('.');
            if (i > 0) ext = originalName.substring(i);
            
            String newFileName = id + ext;
            File destinationFile = new File(getProjectPhotoFolder() + newFileName);

            Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String finalPathForDB = destinationFile.getAbsolutePath().replace("\\", "/");

            // 2. SIMPAN KE FUSEKI
            Employee emp = new Employee();
            emp.setId(id);
            emp.setName(name);
            emp.setPhotoPath(finalPathForDB);
            emp.setDailyTarget("-"); // Default
            
            sparqlService.saveEmployee(emp);

            // 3. SUKSES & KONFIRMASI
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sukses");
            alert.setHeaderText(null);
            alert.setContentText("Karyawan " + name + " berhasil didaftarkan!\nID: " + id);
            alert.showAndWait();

            // Opsi A: Tetap di halaman ini untuk daftar lagi (Reset Form)
            lblStatus.setText("Sukses! ID " + id + " terdaftar.");
            lblStatus.setStyle("-fx-text-fill: green;");
            txtName.clear();
            imgPreview.setImage(null);
            selectedFile = null;
            generateNewId(); 

            // Opsi B: Kalau mau langsung balik ke login otomatis, uncomment baris bawah ini:
            // handleBackToLogin();

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Gagal: " + e.getMessage());
        }
    }
}