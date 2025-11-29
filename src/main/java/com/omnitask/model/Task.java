package com.omnitask.model;

import java.time.LocalDate;

public class Task {
    private String id;
    private String employeeId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private TaskStatus status; // Perhatikan tipe datanya bukan String biasa
    private int progressPercentage;
    private String proofOfWorkPath;
    private String dailyNotes;

    // --- ENUM INI YANG DICARI OLEH TASKSERVICE ---
    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, NEEDS_REVISION
    }
    // ---------------------------------------------

    public Task() {}

    public Task(String id, String employeeId, String title, String description, LocalDate dueDate) {
        this.id = id;
        this.employeeId = employeeId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = TaskStatus.PENDING;
        this.progressPercentage = 0;
    }

    // Getters Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getProofOfWorkPath() { return proofOfWorkPath; }
    public void setProofOfWorkPath(String proofOfWorkPath) { this.proofOfWorkPath = proofOfWorkPath; }
    public String getDailyNotes() { return dailyNotes; }
    public void setDailyNotes(String dailyNotes) { this.dailyNotes = dailyNotes; }
}