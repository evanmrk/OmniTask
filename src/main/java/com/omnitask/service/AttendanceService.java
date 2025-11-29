package com.omnitask.service;

import com.omnitask.model.Attendance;
import com.omnitask.model.Location;
import java.time.LocalDateTime;
import java.util.UUID;

public class AttendanceService {

    private BiometricService biometricService;
    private GeofenceService geofenceService;
    private SPARQLService sparqlService;

    public AttendanceService() {
        this.biometricService = new BiometricService();
        this.geofenceService = new GeofenceService();
        this.sparqlService = new SPARQLService();
    }

    public Attendance checkIn(String employeeId, Location location, String photoPath) throws Exception {
        // 1. Validasi Wajah (Panggil Python)
        System.out.println("Verifying Face for: " + employeeId);
        boolean isFaceValid = biometricService.verifyFace(photoPath, employeeId);

        if (!isFaceValid) {
            throw new Exception("Face Verification Failed! Wajah tidak cocok dengan data karyawan.");
        }

        // 2. Validasi Lokasi (UPDATED: Menggunakan method isWithinGeofence milik Anda)
        boolean isLocationValid = geofenceService.isWithinGeofence(location);

        if (!isLocationValid) {
            throw new Exception("Location Error! Anda berada di luar radius kantor.");
        }

        // 3. Cek apakah sudah absen hari ini?
        Attendance existing = sparqlService.getTodayAttendance(employeeId);
        if (existing != null) {
            throw new Exception("Anda sudah melakukan Check-In hari ini pada jam " +
                    existing.getCheckInTime().toLocalTime());
        }

        // 4. Buat Object Attendance Baru
        Attendance attendance = new Attendance();
        attendance.setId(UUID.randomUUID().toString());
        attendance.setEmployeeId(employeeId);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setLocation(location);
        attendance.setPhotoPath(photoPath);
        // Status LATE/ON_TIME dihitung otomatis di dalam Model Attendance

        // 5. Simpan ke Database Fuseki
        sparqlService.saveAttendance(attendance);

        return attendance;
    }

    public Attendance checkOut(String employeeId, Location location, String photoPath) throws Exception {
        // 1. Validasi Wajah lagi
        boolean isFaceValid = biometricService.verifyFace(photoPath, employeeId);
        if (!isFaceValid) {
            throw new Exception("Face Verification Failed saat Check-Out!");
        }

        // 2. Cari data Check-In hari ini
        Attendance attendance = sparqlService.getTodayAttendance(employeeId);
        if (attendance == null) {
            throw new Exception("Data Check-In hari ini tidak ditemukan. Tidak bisa Check-Out.");
        }

        // 3. Update waktu pulang
        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setStatus("COMPLETED");

        // 4. Update ke Database
        sparqlService.updateCheckOut(attendance.getId(), attendance.getCheckOutTime());

        return attendance;
    }
}