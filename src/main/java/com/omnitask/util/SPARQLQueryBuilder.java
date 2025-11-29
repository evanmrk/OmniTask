package com.omnitask.util;

public class SPARQLQueryBuilder {

    private static final String PREFIXES =
            "PREFIX omni: <http://omnitask.com/ontology#> " +
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";

    public static String insertAttendance(String id, String employeeId, String timestamp,
                                          String lat, String lon, String photoPath, String status) {
        return PREFIXES +
                "INSERT DATA { " +
                "  omni:att_" + id + " rdf:type omni:Attendance ; " +
                "    omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "    omni:hasTimestamp \"" + timestamp + "\"^^xsd:dateTime ; " +
                "    omni:hasGeoLocation \"" + lat + "," + lon + "\" ; " +
                "    omni:hasPhotoPath \"" + photoPath + "\" ; " +
                "    omni:hasStatus \"" + status + "\" . " +
                "}";
    }

    // Kita bisa tambahkan query lain di sini nanti
    public static String getEmployeeByFaceId(String employeeId) {
        return PREFIXES +
                "SELECT ?name ?role WHERE { " +
                "  ?emp rdf:type omni:Employee ; " +
                "       omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "       omni:hasName ?name ; " +
                "       omni:hasRole ?role . " +
                "}";
    }
}