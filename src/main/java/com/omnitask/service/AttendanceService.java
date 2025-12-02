package com.omnitask.service;

import java.time.LocalDateTime;
import java.time.LocalTime; // Import baru untuk jam
import java.util.UUID;

import com.omnitask.model.Attendance;
import com.omnitask.model.Location;

public class AttendanceService {

    private GeofenceService geofenceService;
    private SPARQLService sparqlService;

    // --- KONFIGURASI JAM MASUK (Bisa diubah sesuai kebutuhan) ---
    private static final LocalTime JAM_MASUK = LocalTime.of(9, 0); // 09:00 Pagi

    public AttendanceService() {
        this.geofenceService = new GeofenceService();
        this.sparqlService = new SPARQLService();
    }

    public Attendance checkIn(String employeeId, Location location, String photoPath) throws Exception {
        // 1. Cek apakah sudah absen hari ini?
        Attendance existing = sparqlService.getTodayAttendance(employeeId);
        if (existing != null) {
            // Jika statusnya bukan SICK/PERMIT, tolak check-in ganda
            throw new Exception("Anda sudah absen hari ini (Status: " + existing.getStatus() +
                    ") pada jam " + existing.getCheckInTime().toLocalTime());
        }

        // 2. Buat Data Absensi Baru
        Attendance attendance = new Attendance();
        String attId = "ATT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        attendance.setId(attId);
        attendance.setEmployeeId(employeeId);
        attendance.setCheckInTime(now);
        attendance.setLocation(location);
        attendance.setPhotoPath(photoPath);

        // --- 3. LOGIKA PENENTUAN STATUS (PRESENT vs LATE) ---
        // Jika waktu check-in (now) lebih dari jam 09:00, maka LATE
        if (now.toLocalTime().isAfter(JAM_MASUK)) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        // 4. Simpan ke Database
        sparqlService.saveAttendance(attendance);
        System.out.println("DEBUG: Check-In Sukses (" + attendance.getStatus() + ") ID: " + attId);

        return attendance;
    }

    public Attendance checkOut(String employeeId, Location location, String photoPath) throws Exception {
        Attendance existing = sparqlService.getTodayAttendance(employeeId);

        // Jika tidak ada data hari ini (return null), berarti user belum Check-In
        if (existing == null) {
            System.err.println("DEBUG: Gagal CheckOut. Tidak ada data CheckIn hari ini untuk: " + employeeId);
            throw new Exception("Gagal: Data Check-In hari ini tidak ditemukan. Harap Check-In dulu.");
        }

        // 2. Cek apakah SUDAH Check-Out sebelumnya?
        if (existing.getCheckOutTime() != null) {
            throw new Exception("Anda sudah Check-Out sebelumnya pada jam " + existing.getCheckOutTime().toLocalTime());
        }

        // 3. Update Waktu Check-Out
        LocalDateTime now = LocalDateTime.now();
        existing.setCheckOutTime(now);

        // 4. Update ke Database
        sparqlService.updateCheckOut(existing.getId(), now);

        System.out.println("DEBUG: Check-Out Sukses untuk Absen ID: " + existing.getId());

        return existing;
    }
}