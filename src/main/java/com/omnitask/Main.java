package com.omnitask;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.omnitask.config.AppConfig;
import com.omnitask.config.RDFConfig;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Initialize configurations
            System.out.println("Initializing OmniTask Configurations...");
            AppConfig.initialize();
            RDFConfig.initialize();

            // --- PERUBAHAN DI SINI ---
            
            // Pastikan nama file SAMA PERSIS (huruf kecil semua sesuai info kamu)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attendance_view.fxml"));
            Parent root = loader.load();

            // Ubah ukuran window menjadi lebih besar untuk Dashboard
            Scene scene = new Scene(root, 1000, 700);

            // Opsional: Load CSS jika ada
            // if (getClass().getResource("/css/styles.css") != null) {
            //    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            // }

            primaryStage.setTitle("OmniTask - Employee Attendance System");
            primaryStage.setScene(scene);
            
            // Izinkan window di-resize (PENTING untuk dashboard)
            primaryStage.setResizable(true); 
            
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("FATAL ERROR: Failed to start application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Cleanup resources saat aplikasi ditutup
        RDFConfig.shutdown();
        System.out.println("Application closed successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}