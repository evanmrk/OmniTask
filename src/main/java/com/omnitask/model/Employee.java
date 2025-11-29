package com.omnitask.model;

import java.util.Objects;

public class Employee {
    private String id;
    private String name;
    private String email;
    private String department;
    private String role;
    private String photoPath;    // Penting untuk menampilkan foto profil
    private String dailyTarget;  // Penting untuk menampilkan target kerja
    private boolean isManager;

    // Constructor Kosong (Wajib untuk beberapa library)
    public Employee() {}

    // Constructor Lengkap
    public Employee(String id, String name, String email, String department, String role, String photoPath, String dailyTarget) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.role = role;
        this.photoPath = photoPath;
        this.dailyTarget = dailyTarget;
        this.isManager = false; // Default false, bisa diubah via setter
    }

    // --- GETTERS AND SETTERS ---

    public String getId() { // Perbaikan huruf besar 'I'
        return id;
    }

    public void setId(String id) { // Perbaikan huruf besar 'I'
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhotoPath() { // Perbaikan huruf besar 'P'
        return photoPath;
    }

    public void setPhotoPath(String photoPath) { // Perbaikan huruf besar 'P'
        this.photoPath = photoPath;
    }

    public String getDailyTarget() { // Penambahan Getter
        return dailyTarget;
    }

    public void setDailyTarget(String dailyTarget) { // Penambahan Setter
        this.dailyTarget = dailyTarget;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean isManager) {
        this.isManager = isManager;
    }

    // --- EQUALS, HASHCODE, TOSTRING ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(id, employee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dailyTarget='" + dailyTarget + '\'' +
                ", photoPath='" + photoPath + '\'' +
                '}';
    }
}