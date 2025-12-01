package com.omnitask.service;

import com.omnitask.model.Employee;

public class SessionManager {

    // Variabel statis: Datanya akan bertahan selama aplikasi jalan
    private static Employee currentUser;

    // Simpan data user saat login
    public static void login(Employee employee) {
        currentUser = employee;
    }

    // Hapus data saat logout
    public static void logout() {
        currentUser = null;
    }

    // Ambil data user saat ini
    public static Employee getCurrentUser() {
        return currentUser;
    }

    // Cek status apakah sedang login
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}