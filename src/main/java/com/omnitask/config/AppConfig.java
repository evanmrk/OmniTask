package com.omnitask.config;

import java.io.*;
import java.util.Properties;

public class AppConfig {

    private static Properties properties;
    private static final String CONFIG_FILE = "config/application.properties";

    public static void initialize() {
        properties = new Properties();

        try {
            // Load from classpath (works both in IDE and JAR)
            InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

            if (input == null) {
                System.err.println("Unable to find " + CONFIG_FILE);
                loadDefaults();
                return;
            }

            properties.load(input);
            System.out.println("✓ Configuration loaded successfully");
            input.close();

        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        System.out.println("Loading default configuration...");
        properties.setProperty("app.name", "OmniTask");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("office.latitude", "3.5952");
        properties.setProperty("office.longitude", "98.6722");
        properties.setProperty("geofence.radius.km", "0.5");
        properties.setProperty("python.path", "python");
        properties.setProperty("python.script", "python_service/face_recognition_service.py");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static double getDoubleProperty(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}