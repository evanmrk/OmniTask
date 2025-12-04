package com.omnitask.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class Attendance {
    private String id;
    private String employeeId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String photoPath;
    private Location location;
    private String status; // "ON_TIME", "LATE", "COMPLETED"
    private Location checkInLocation;
    private String checkInPhotoPath;

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(8, 0);

    public Attendance() {}

    public Attendance(String id, String employeeId, LocalDateTime checkInTime, Location location, String photoPath) {
        this.id = id;
        this.employeeId = employeeId;
        this.checkInTime = checkInTime;
        this.location = location;
        this.photoPath = photoPath;
        this.status = calculateStatus(checkInTime);
    }

    private String calculateStatus(LocalDateTime checkIn) {
        if (checkIn.toLocalTime().isAfter(LATE_THRESHOLD)) {
            return "LATE";
        }
        return "ON_TIME";
    }

    public boolean isLate() {
        return "LATE".equalsIgnoreCase(this.status);
    }

    public long getLateMinutes() {
        if (checkInTime == null) return 0;
        long diff = ChronoUnit.MINUTES.between(LATE_THRESHOLD, checkInTime.toLocalTime());
        return diff > 0 ? diff : 0;
    }

    // ==========================================
    // GETTERS & SETTERS LENGKAP
    // ==========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttendanceId() {
        return this.id;  // Return id yang sama
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
        if(checkInTime != null) {
            this.status = calculateStatus(checkInTime);
        }
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Location getCheckInLocation() {
        return checkInLocation;
    }

    public void setCheckInLocation(Location checkInLocation) {
        this.checkInLocation = checkInLocation;
    }

    public String getCheckInPhotoPath() {
        return checkInPhotoPath;
    }

    public void setCheckInPhotoPath(String checkInPhotoPath) {
        this.checkInPhotoPath = checkInPhotoPath;
    }

    // ==========================================
    // METHOD "JEMBATAN" (WRAPPER)
    // Untuk kompatibilitas dengan SPARQLService
    // ==========================================

    public double getLatitude() {
        return (location != null) ? location.getLatitude() : 0.0;
    }

    public double getLongitude() {
        return (location != null) ? location.getLongitude() : 0.0;
    }

    public void setLatitude(double lat) {
        if (this.location == null) {
            this.location = new Location(lat, 0);
        } else {
            this.location.setLatitude(lat);
        }
    }

    public void setLongitude(double lon) {
        if (this.location == null) {
            this.location = new Location(0, lon);
        } else {
            this.location.setLongitude(lon);
        }
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id='" + id + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", checkInTime=" + checkInTime +
                ", status='" + status + '\'' +
                ", isLate=" + isLate() +
                '}';
    }
}