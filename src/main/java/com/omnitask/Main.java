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

            // 2. Load main view (attendance view as default)
            // Pastikan path "/fxml/attendance_view.fxml" sesuai dengan struktur folder resources Anda
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attendance_view.fxml"));
            Parent root = loader.load();

            // 3. Setup Scene
            Scene scene = new Scene(root, 1200, 800);

            // Load CSS (dengan null check agar aman)
//            if (getClass().getResource("/css/styles.css") != null) {
//                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
//            } else {
//                System.err.println("Warning: styles.css not found!");
//            }

            primaryStage.setTitle("OmniTask - Employee Management System");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
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