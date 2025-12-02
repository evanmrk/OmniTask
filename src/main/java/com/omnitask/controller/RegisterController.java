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
        generateNewId();
    }

    private void generateNewId() {
        String uniqueCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        txtId.setText("EMP-" + uniqueCode);
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
            lblStatus.setStyle("-fx-text-fill: blue;");
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty() || selectedFile == null) {
            lblStatus.setText("Nama dan Foto wajib diisi!");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();

            // 1. Simpan Foto
            String originalName = selectedFile.getName();
            String ext = "";
            int i = originalName.lastIndexOf('.');
            if (i > 0) ext = originalName.substring(i);

            String newFileName = id + ext;
            File destinationFile = new File(getProjectPhotoFolder() + newFileName);

            Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String finalPathForDB = destinationFile.getAbsolutePath().replace("\\", "/");

            // 2. Simpan ke Database RDF
            Employee emp = new Employee();
            emp.setId(id);
            emp.setName(name);
            emp.setPhotoPath(finalPathForDB);
            // Role & Dept bisa ditambahkan input fieldnya nanti jika mau
            emp.setRole("Staff");
            emp.setDepartment("General");
            emp.setDailyTarget("-");

            sparqlService.saveEmployee(emp);

            // 3. Feedback Sukses
            lblStatus.setText("Sukses! ID " + id + " tersimpan.");
            lblStatus.setStyle("-fx-text-fill: green;");

            // Reset Form untuk input berikutnya (Opsional, atau bisa langsung close)
            txtName.clear();
            imgPreview.setImage(null);
            selectedFile = null;
            generateNewId();

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Gagal: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    // --- HANDLE TUTUP WINDOW ---
    @FXML
    private void handleClose() {
        try {
            Stage stage = (Stage) txtId.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}