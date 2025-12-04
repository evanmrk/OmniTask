package com.omnitask;

import com.omnitask.model.Employee;
import com.omnitask.service.SPARQLService;
import com.omnitask.config.RDFConfig;

public class SetupManager {
    public static void main(String[] args) {
        System.out.println("Memulai proses pendaftaran Manager...");

        RDFConfig.initialize();
        SPARQLService service = new SPARQLService();

        //Create Data Manager
        Employee manager = new Employee();
        manager.setId("EMP-1X8W91");
        manager.setName("Manager REA");
        manager.setRole("Manager");


        manager.setPhotoPath("");

        service.saveEmployee(manager);

        System.out.println("SUKSES: Akun Manager (ID: EMP-1X8W91) berhasil dibuat!");

        RDFConfig.shutdown();
    }
}