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
    @FXML private Label lblStatus;

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
    private void handleSave() {
        if (txtName.getText().isEmpty()) {
            lblStatus.setText("Nama wajib diisi!");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }


        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();

            // LOGIKA COPY FILE DIHAPUS SEMUA DI SINI

            // Simpan ke Database RDF
            Employee emp = new Employee();
            emp.setId(id);
            emp.setName(name);

            // Set "-" (strip) karena foto dihapus, agar database tidak error
            emp.setPhotoPath("-");

            emp.setRole("Staff");
            emp.setDepartment("General");
            emp.setDailyTarget("-");

            sparqlService.saveEmployee(emp);

            lblStatus.setText("Sukses! ID " + id + " tersimpan.");
            lblStatus.setStyle("-fx-text-fill: green;");

            txtName.clear();
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