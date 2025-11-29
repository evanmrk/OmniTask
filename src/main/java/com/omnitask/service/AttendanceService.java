package com.omnitask.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.omnitask.model.Attendance;
import com.omnitask.model.Location;

public class AttendanceService {

    // Hapus BiometricService
    private GeofenceService geofenceService;
    private SPARQLService sparqlService;

    public AttendanceService() {
        // Hapus inisialisasi BiometricService
        this.geofenceService = new GeofenceService();
        this.sparqlService = new SPARQLService();
    }

    public Attendance checkIn(String employeeId, Location location, String photoPath) throws Exception {
        // --- BAGIAN BIOMETRIC DIHAPUS ---
        
        // 1. Validasi Lokasi (Wajib di Kantor)
        boolean isLocationValid = geofenceService.isWithinGeofence(location);

        if (!isLocationValid) {
            throw new Exception("Location Error! Anda berada di luar radius kantor.");
        }

        // 2. Cek apakah sudah absen hari ini?
        Attendance existing = sparqlService.getTodayAttendance(employeeId);
        if (existing != null) {
            throw new Exception("Anda sudah melakukan Check-In hari ini pada jam " +
                    existing.getCheckInTime().toLocalTime());
        }

        // 3. Buat Object Attendance Baru
        Attendance attendance = new Attendance();
        attendance.setId(UUID.randomUUID().toString());
        attendance.setEmployeeId(employeeId);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setLocation(location);
        
        // Simpan path foto (yang diambil dari database profil) ke data absen sebagai histori
        attendance.setPhotoPath(photoPath); 
        
        // Status LATE/ON_TIME dihitung otomatis di dalam Model Attendance saat setCheckInTime

        // 4. Simpan ke Database Fuseki
        sparqlService.saveAttendance(attendance);

        return attendance;
    }

    public Attendance checkOut(String employeeId, Location location, String photoPath) throws Exception {
        // --- BAGIAN BIOMETRIC DIHAPUS ---

        // Opsional: Anda bisa menambahkan validasi lokasi juga di sini jika mau
        // boolean isLocationValid = geofenceService.isWithinGeofence(location);
        // if (!isLocationValid) throw new Exception("Harus di kantor untuk Check-Out!");

        // 1. Cari data Check-In hari ini
        Attendance attendance = sparqlService.getTodayAttendance(employeeId);
        if (attendance == null) {
            throw new Exception("Data Check-In hari ini tidak ditemukan. Tidak bisa Check-Out.");
        }

        // 2. Update waktu pulang
        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setStatus("COMPLETED");

        // 3. Update ke Database
        sparqlService.updateCheckOut(attendance.getId(), attendance.getCheckOutTime());

        return attendance;
    }
}