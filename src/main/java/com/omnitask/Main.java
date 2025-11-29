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

            // 2. Load MAIN LAYOUT (Cangkang Utama)
            // Perhatikan: Kita meload MainLayout, bukan AttendanceScreen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            // 3. Setup Scene
            Scene scene = new Scene(root, 1200, 800);

            // Load CSS (Jika nanti sudah ada file CSS-nya, uncomment ini)
            // if (getClass().getResource("/css/styles.css") != null) {
            //    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            // }

            primaryStage.setTitle("OmniTask - Employee Management System");
            primaryStage.setScene(scene);

            // Opsional: Agar aplikasi terbuka full screen
            primaryStage.setMaximized(true);

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("FATAL ERROR: Failed to start application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // PENTING: Tetap pertahankan ini agar database Fuseki/TDB tertutup rapi
        RDFConfig.shutdown();
        System.out.println("Application closed successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}