package com.omnitask.config;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdf.model.*;

public class RDFConfig {

    // HAPUS variabel static connection
    // private static RDFConnection connection; <--- HAPUS INI

    private static final String FUSEKI_URL = "http://localhost:3030/omnitask";
    private static final String NS = "http://omnitask.com/ontology#";

    public static void initialize() {
        // Kita buka koneksi sementara hanya untuk cek status server
        try (RDFConnection conn = getConnection()) { 
            
            System.out.println("✓ Connected to Fuseki server at " + FUSEKI_URL);

            // Test connection
            conn.querySelect("SELECT * WHERE { ?s ?p ?o } LIMIT 1", rs -> {
                System.out.println("✓ Fuseki connection test successful");
            });

            // Load ontology if needed
            loadOntologyIfNeeded(conn);

        } catch (Exception e) {
            System.err.println("✗ Error connecting to Fuseki: " + e.getMessage());
            System.err.println("Make sure Fuseki is running at http://localhost:3030 and dataset 'omnitask' exists.");
            e.printStackTrace();
        }
    }

    // --- BAGIAN TERPENTING: SELALU BUAT KONEKSI BARU ---
    public static RDFConnection getConnection() {
        return RDFConnectionFactory.connect(FUSEKI_URL);
    }

    private static void loadOntologyIfNeeded(RDFConnection conn) {
        try {
            // Check if ontology already exists
            boolean hasData = conn.queryAsk("ASK { ?s ?p ?o }");

            if (!hasData) {
                System.out.println("Loading base ontology...");
                createBaseOntology(conn);
            } else {
                System.out.println("✓ Ontology already exists in Fuseki");
            }
        } catch (Exception e) {
            System.err.println("Error checking/loading ontology: " + e.getMessage());
        }
    }

    private static void createBaseOntology(RDFConnection conn) {
        Model model = ModelFactory.createDefaultModel();

        // Create base classes
        Resource employeeClass = model.createResource(NS + "Employee");
        Resource attendanceClass = model.createResource(NS + "Attendance");
        Resource taskClass = model.createResource(NS + "Task");
        
        // Create properties (SAYA UPDATE SESUAI FITUR BARU)
        model.createProperty(NS + "hasID");
        model.createProperty(NS + "name");           // diganti jadi simple 'name' sesuai query insert kita
        model.createProperty(NS + "photoPath");      // PENTING: Untuk foto portable
        model.createProperty(NS + "dailyTarget");    // PENTING: Untuk target kerja
        
        model.createProperty(NS + "checkInTime");
        model.createProperty(NS + "checkOutTime");
        model.createProperty(NS + "latitude");
        model.createProperty(NS + "longitude");
        model.createProperty(NS + "status");

        // Upload to Fuseki
        conn.load(model);
        System.out.println("✓ Base ontology created in Fuseki");
    }

    public static String getNamespace() {
        return NS;
    }

    public static void shutdown() {
        // Tidak perlu close manual disini karena kita close per request
        System.out.println("RDFConfig shutdown signal received.");
    }
}