package com.omnitask.config;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;

public class RDFConfig {

    private static RDFConnection connection;
    private static final String FUSEKI_URL = "http://localhost:3030/omnitask";
    private static final String NS = "http://omnitask.com/ontology#";

    public static void initialize() {
        try {
            // Connect to Fuseki server
            connection = RDFConnectionFactory.connect(FUSEKI_URL);
            System.out.println("✓ Connected to Fuseki server at " + FUSEKI_URL);

            // Test connection
            connection.querySelect("SELECT * WHERE { ?s ?p ?o } LIMIT 1", rs -> {
                System.out.println("✓ Fuseki connection test successful");
            });

            // Load ontology if needed
            loadOntologyIfNeeded();

        } catch (Exception e) {
            System.err.println("✗ Error connecting to Fuseki: " + e.getMessage());
            System.err.println("Make sure Fuseki is running at http://localhost:3030");
            e.printStackTrace();
        }
    }

    private static void loadOntologyIfNeeded() {
        try {
            // Check if ontology already exists
            boolean hasData = connection.queryAsk("ASK { ?s ?p ?o }");

            if (!hasData) {
                System.out.println("Loading base ontology...");
                createBaseOntology();
            } else {
                System.out.println("✓ Ontology already exists in Fuseki");
            }
        } catch (Exception e) {
            System.err.println("Error checking/loading ontology: " + e.getMessage());
        }
    }

    private static void createBaseOntology() {
        Model model = ModelFactory.createDefaultModel();

        // Create base classes
        Resource employeeClass = model.createResource(NS + "Employee");
        Resource attendanceClass = model.createResource(NS + "Attendance");
        Resource taskClass = model.createResource(NS + "Task");
        Resource leaveRequestClass = model.createResource(NS + "LeaveRequest");

        // Create properties
        model.createProperty(NS + "hasEmployeeId");
        model.createProperty(NS + "hasName");
        model.createProperty(NS + "hasEmail");
        model.createProperty(NS + "hasDepartment");
        model.createProperty(NS + "hasRole");
        model.createProperty(NS + "hasFaceEncoding");
        model.createProperty(NS + "hasCheckInTime");
        model.createProperty(NS + "hasCheckOutTime");
        model.createProperty(NS + "hasGeoLocation");
        model.createProperty(NS + "hasTaskStatus");
        model.createProperty(NS + "hasProgress");

        // Upload to Fuseki
        connection.load(model);
        System.out.println("✓ Base ontology created in Fuseki");
    }

    public static RDFConnection getConnection() {
        return connection;
    }

    public static String getNamespace() {
        return NS;
    }

    public static void shutdown() {
        if (connection != null) {
            connection.close();
            System.out.println("✓ Fuseki connection closed");
        }
    }
}