package com.omnitask;

import com.omnitask.model.Employee;
import com.omnitask.service.SPARQLService;
import com.omnitask.config.RDFConfig;

public class SetupManager {
    public static void main(String[] args) {
        System.out.println("Memulai proses pendaftaran Manager...");

        // 1. Inisialisasi Koneksi ke Database Fuseki
        RDFConfig.initialize();
        SPARQLService service = new SPARQLService();

        // 2. Buat Data Manager
        Employee manager = new Employee();
        manager.setId("EMP-1X8W91");     // ID Manager yang Anda tentukan
        manager.setName("Manager Admin"); // Nama Manager
        manager.setRole("Manager");      // PENTING: Ini kunci agar tombol "Employees" muncul

        // Field Department, Email, dan Target TIDAK DISET (akan dianggap kosong/- oleh sistem)

        // Path Foto
        // Pastikan file gambar ini benar-benar ada di komputer Anda agar tidak error saat load gambar
        // Jika belum ada foto, bisa dikosongkan dulu: manager.setPhotoPath("");
        manager.setPhotoPath("");

        // 3. Simpan ke Database
        service.saveEmployee(manager);

        System.out.println("SUKSES: Akun Manager (ID: EMP-1X8W91) berhasil dibuat!");

        // Matikan koneksi
        RDFConfig.shutdown();
    }
}