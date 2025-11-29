package com.omnitask.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Default tampilan saat buka aplikasi
        showAttendance();
    }

    @FXML
    private void showAttendance() {
        loadView("/fxml/AttendanceScreen.fxml");
    }

    @FXML
    private void showTasks() {
        loadView("/fxml/TaskOverview.fxml"); // Pastikan nama file ini sesuai dgn file Task kamu
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Bersihkan area tengah, lalu isi dengan view baru
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Gagal memuat view: " + fxmlPath);
        }
    }
}