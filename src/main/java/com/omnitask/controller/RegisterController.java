package com.omnitask.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.omnitask.model.Employee;
import com.omnitask.service.SPARQLService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView; // Jangan lupa import UUID
import javafx.stage.FileChooser;

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
        // Buat UUID acak, ambil 5 karakter pertama, jadikan huruf besar
        String uniqueCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        
        // Gabungkan dengan prefix "EMP-"
        String autoId = "EMP-" + uniqueCode; 
        
        // Masukkan ke TextField
        txtId.setText(autoId);
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
        // Validasi: ID pasti ada karena otomatis. Cek Nama dan Foto saja.
        if (txtName.getText().isEmpty() || selectedFile == null) {
            lblStatus.setText("Error: Nama dan Foto wajib diisi!");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            // Ambil ID yang sudah digenerate otomatis tadi
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

            // 3. SUKSES & RESET
            lblStatus.setText("Sukses! ID " + id + " terdaftar.");
            lblStatus.setStyle("-fx-text-fill: green;");
            
            txtName.clear();
            imgPreview.setImage(null);
            selectedFile = null;
            
            // Generate ID baru lagi untuk karyawan berikutnya
            generateNewId(); 

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Gagal: " + e.getMessage());
        }
    }
}