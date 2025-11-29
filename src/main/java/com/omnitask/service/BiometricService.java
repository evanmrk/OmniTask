package com.omnitask.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BiometricService {

    private static final String PYTHON_SCRIPT_PATH = "python_service/face_recognition_service.py";
    private static final String PYTHON_COMMAND = "python";

    /**
     * Verifies face by calling Python face recognition service
     * @param capturedImagePath Path to the captured image
     * @param employeeId Employee ID to match against stored face
     * @return true if face matches, false otherwise
     */
    public boolean verifyFace(String capturedImagePath, String employeeId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_COMMAND,
                    PYTHON_SCRIPT_PATH,
                    capturedImagePath,
                    employeeId
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("Python output: " + line);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                String result = output.toString().trim();
                return result.contains("MATCH") || result.contains("True");
            } else {
                System.err.println("Python script failed with exit code: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error during face verification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registers a new face for an employee
     * @param imagePath Path to the face image
     * @param employeeId Employee ID
     * @return true if registration successful
     */
    public boolean registerFace(String imagePath, String employeeId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_COMMAND,
                    PYTHON_SCRIPT_PATH,
                    "--register",
                    imagePath,
                    employeeId
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Registration output: " + line);
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            System.err.println("Error during face registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}