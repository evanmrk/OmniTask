package com.omnitask.service;

import com.omnitask.model.Employee;

public class SessionManager {

    private static Employee currentUser;

    public static void login(Employee employee) {
        currentUser = employee;
    }

    public static void logout() {
        currentUser = null;
    }

    public static Employee getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}