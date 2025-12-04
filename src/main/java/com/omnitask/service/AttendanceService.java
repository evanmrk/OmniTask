package com.omnitask.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.omnitask.model.Attendance;
import com.omnitask.model.Location;

public class AttendanceService {

    private GeofenceService geofenceService;
    private SPARQLService sparqlService;

    // --- KONFIGURASI JAM ---
    private static final LocalTime JAM_MASUK = LocalTime.of(9, 0);      // 09:00 Pagi (Batas Terlambat)
    private static final LocalTime JAM_BATAS_AKHIR = LocalTime.of(23, 0); // 16:00 Sore (Batas Absen Masuk)

    public AttendanceService() {
        this.geofenceService = new GeofenceService();
        this.sparqlService = new SPARQLService();
    }

    public Attendance checkIn(String employeeId, Location location, String photoPath) throws Exception {
        // Ambil waktu sekarang
        LocalDateTime now = LocalDateTime.now();

        // --- 1. VALIDASI JAM KERJA (FITUR BARU) ---
        // Jika waktu sekarang LEBIH DARI jam 16:00, tolak akses.
        if (now.toLocalTime().isAfter(JAM_BATAS_AKHIR)) {
            throw new Exception("DILUAR JAM KERJA");
        }

        // 2. Cek apakah sudah absen hari ini?
        Attendance existing = sparqlService.getTodayAttendance(employeeId);
        if (existing != null) {
            throw new Exception("Anda sudah absen hari ini (Status: " + existing.getStatus() +
                    ") pada jam " + existing.getCheckInTime().toLocalTime());
        }

        // 3. Buat Data Absensi Baru
        Attendance attendance = new Attendance();
        String attId = "ATT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        attendance.setId(attId);
        attendance.setEmployeeId(employeeId);
        attendance.setCheckInTime(now);
        attendance.setLocation(location);
        attendance.setPhotoPath(photoPath);

        // 4. Logika Penentuan Status (PRESENT vs LATE)
        if (now.toLocalTime().isAfter(JAM_MASUK)) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        // 5. Simpan ke Database
        sparqlService.saveAttendance(attendance);
        System.out.println("DEBUG: Check-In Sukses (" + attendance.getStatus() + ") ID: " + attId);

        return attendance;
    }

    public Attendance checkOut(String employeeId, Location location, String photoPath) throws Exception {
        Attendance existing = sparqlService.getTodayAttendance(employeeId);

        if (existing == null) {
            System.err.println("DEBUG: Gagal CheckOut. Tidak ada data CheckIn hari ini untuk: " + employeeId);
            throw new Exception("Gagal: Data Check-In hari ini tidak ditemukan. Harap Check-In dulu.");
        }

        if (existing.getCheckOutTime() != null) {
            throw new Exception("Anda sudah Check-Out sebelumnya pada jam " + existing.getCheckOutTime().toLocalTime());
        }

        LocalDateTime now = LocalDateTime.now();
        existing.setCheckOutTime(now);

        sparqlService.updateCheckOut(existing.getId(), now);

        System.out.println("DEBUG: Check-Out Sukses untuk Absen ID: " + existing.getId());

        return existing;
    }
}