package com.omnitask.service;

import com.omnitask.config.RDFConfig;
import com.omnitask.model.Attendance;
import com.omnitask.model.Location;
import com.omnitask.model.Task;
import com.omnitask.model.Task.TaskStatus;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class SPARQLService {

    private RDFConnection getConnection() {
        return RDFConfig.getConnection();
    }

    private static final String NS = RDFConfig.getNamespace();

    private static final String PREFIXES =
            "PREFIX omni: <http://omnitask.com/ontology#> " +
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ==========================================
    // ATTENDANCE METHODS
    // ==========================================

    public void saveAttendance(Attendance attendance) {
        RDFConnection conn = getConnection();
        if (conn == null) {
            System.err.println("No Fuseki connection available");
            return;
        }

        Model model = ModelFactory.createDefaultModel();
        String attendanceURI = NS + "attendance_" + attendance.getAttendanceId();
        Resource attendanceRes = model.createResource(attendanceURI);

        Property hasEmployeeId = model.createProperty(NS + "hasEmployeeId");
        Property hasCheckInTime = model.createProperty(NS + "hasCheckInTime");
        Property hasCheckInLat = model.createProperty(NS + "hasCheckInLatitude");
        Property hasCheckInLon = model.createProperty(NS + "hasCheckInLongitude");
        Property hasCheckInPhoto = model.createProperty(NS + "hasCheckInPhoto");
        Property isLate = model.createProperty(NS + "isLate");
        Property lateMinutes = model.createProperty(NS + "lateMinutes");
        Property hasStatus = model.createProperty(NS + "hasStatus");

        attendanceRes.addProperty(hasEmployeeId, attendance.getEmployeeId());
        attendanceRes.addProperty(hasCheckInTime, attendance.getCheckInTime().toString());
        attendanceRes.addProperty(hasCheckInLat, String.valueOf(attendance.getCheckInLocation().getLatitude()));
        attendanceRes.addProperty(hasCheckInLon, String.valueOf(attendance.getCheckInLocation().getLongitude()));
        attendanceRes.addProperty(hasCheckInPhoto, attendance.getCheckInPhotoPath());
        attendanceRes.addProperty(isLate, String.valueOf(attendance.isLate()));
        attendanceRes.addProperty(lateMinutes, String.valueOf(attendance.getLateMinutes()));
        attendanceRes.addProperty(hasStatus, attendance.getStatus().toString());

        // Upload to Fuseki
        conn.load(model);
        System.out.println("✓ Attendance saved to Fuseki");
    }

    public void updateCheckOut(String attendanceId, LocalDateTime checkOutTime) {
        String updateString = PREFIXES +
                "DELETE { ?s omni:checkOutTime ?old } " +
                "INSERT { ?s omni:checkOutTime \"" + checkOutTime.format(DATETIME_FMT) + "\"^^xsd:dateTime } " +
                "WHERE { " +
                "  BIND(omni:att_" + attendanceId + " AS ?s) " +
                "  OPTIONAL { ?s omni:checkOutTime ?old } " +
                "}";

        executeUpdate(updateString);
    }

    public Attendance getTodayAttendance(String employeeId) {
        String today = LocalDate.now().toString(); // YYYY-MM-DD

        String queryString = PREFIXES +
                "SELECT ?id ?checkIn ?lat ?lon ?photo ?status ?checkOut WHERE { " +
                "  ?s rdf:type omni:Attendance ; " +
                "     omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "     omni:checkInTime ?checkIn ; " +
                "     omni:latitude ?lat ; " +
                "     omni:longitude ?lon ; " +
                "     omni:photoPath ?photo ; " +
                "     omni:status ?status . " +
                "  BIND(STRAFTER(STR(?s), \"att_\") AS ?id) " +
                "  OPTIONAL { ?s omni:checkOutTime ?checkOut } " +
                "  FILTER(STRSTARTS(STR(?checkIn), \"" + today + "\")) " +
                "} LIMIT 1";

        try (RDFConnection conn = RDFConfig.getConnection();
             QueryExecution qExec = conn.query(queryString)) {

            ResultSet rs = qExec.execSelect();
            if (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Attendance att = new Attendance();
                att.setId(soln.getLiteral("id").getString());
                att.setEmployeeId(employeeId);
                att.setCheckInTime(LocalDateTime.parse(soln.getLiteral("checkIn").getString(), DATETIME_FMT));
                att.setLatitude(Double.parseDouble(soln.getLiteral("lat").getString()));
                att.setLongitude(Double.parseDouble(soln.getLiteral("lon").getString()));
                att.setLocation(new Location(att.getLatitude(), att.getLongitude()));
                att.setPhotoPath(soln.getLiteral("photo").getString());
                att.setStatus(soln.getLiteral("status").getString());

                if (soln.contains("checkOut")) {
                    att.setCheckOutTime(LocalDateTime.parse(soln.getLiteral("checkOut").getString(), DATETIME_FMT));
                }
                return att;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==========================================
    // TASK METHODS (Yang dicari TaskService)
    // ==========================================

    public void saveTask(Task task) {
        String updateString = PREFIXES +
                "INSERT DATA { " +
                "  omni:task_" + task.getId() + " rdf:type omni:Task ; " +
                "    omni:hasEmployeeId \"" + task.getEmployeeId() + "\" ; " +
                "    omni:title \"" + task.getTitle() + "\" ; " +
                "    omni:description \"" + task.getDescription() + "\" ; " +
                "    omni:dueDate \"" + task.getDueDate().format(DATE_FMT) + "\"^^xsd:date ; " +
                "    omni:taskStatus \"" + task.getStatus().name() + "\" ; " +
                "    omni:progress \"" + task.getProgressPercentage() + "\"^^xsd:int . " +
                "}";

        executeUpdate(updateString);
    }

    public Task getTask(String taskId) {
        String queryString = PREFIXES +
                "SELECT * WHERE { " +
                "  omni:task_" + taskId + " ?p ?o . " +
                "  omni:task_" + taskId + " omni:hasEmployeeId ?empId ; " +
                "             omni:title ?title ; " +
                "             omni:description ?desc ; " +
                "             omni:dueDate ?due ; " +
                "             omni:taskStatus ?status ; " +
                "             omni:progress ?progress . " +
                "  OPTIONAL { omni:task_" + taskId + " omni:proofPath ?proof } " +
                "  OPTIONAL { omni:task_" + taskId + " omni:notes ?notes } " +
                "}";

        try (RDFConnection conn = RDFConfig.getConnection();
             QueryExecution qExec = conn.query(queryString)) {

            ResultSet rs = qExec.execSelect();
            if (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Task t = new Task();
                t.setId(taskId);
                t.setEmployeeId(soln.getLiteral("empId").getString());
                t.setTitle(soln.getLiteral("title").getString());
                t.setDescription(soln.getLiteral("desc").getString());
                t.setDueDate(LocalDate.parse(soln.getLiteral("due").getString(), DATE_FMT));
                t.setStatus(TaskStatus.valueOf(soln.getLiteral("status").getString()));
                t.setProgressPercentage(soln.getLiteral("progress").getInt());

                if (soln.contains("proof")) t.setProofOfWorkPath(soln.getLiteral("proof").getString());
                if (soln.contains("notes")) t.setDailyNotes(soln.getLiteral("notes").getString());

                return t;
            }
        }
        return null;
    }

    public void updateTask(Task task) {
        String updateString = PREFIXES +
                "DELETE { " +
                "  omni:task_" + task.getId() + " omni:taskStatus ?oldStatus ; " +
                "                               omni:progress ?oldProg . " +
                "  omni:task_" + task.getId() + " omni:proofPath ?oldProof . " +
                "  omni:task_" + task.getId() + " omni:notes ?oldNotes . " +
                "} INSERT { " +
                "  omni:task_" + task.getId() + " omni:taskStatus \"" + task.getStatus().name() + "\" ; " +
                "                               omni:progress \"" + task.getProgressPercentage() + "\"^^xsd:int . " +
                (task.getProofOfWorkPath() != null ? "  omni:task_" + task.getId() + " omni:proofPath \"" + task.getProofOfWorkPath() + "\" . " : "") +
                (task.getDailyNotes() != null ? "  omni:task_" + task.getId() + " omni:notes \"" + task.getDailyNotes() + "\" . " : "") +
                "} WHERE { " +
                "  omni:task_" + task.getId() + " omni:taskStatus ?oldStatus ; " +
                "                               omni:progress ?oldProg . " +
                "  OPTIONAL { omni:task_" + task.getId() + " omni:proofPath ?oldProof } " +
                "  OPTIONAL { omni:task_" + task.getId() + " omni:notes ?oldNotes } " +
                "}";

        executeUpdate(updateString);
    }

    public void deleteTask(String taskId) {
        String updateString = PREFIXES + "DELETE WHERE { omni:task_" + taskId + " ?p ?o }";
        executeUpdate(updateString);
    }

    public List<Task> getTasksForEmployee(String employeeId) {
        String queryString = PREFIXES +
                "SELECT ?s ?title ?desc ?due ?status ?progress WHERE { " +
                "  ?s rdf:type omni:Task ; " +
                "     omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "     omni:title ?title ; " +
                "     omni:description ?desc ; " +
                "     omni:dueDate ?due ; " +
                "     omni:taskStatus ?status ; " +
                "     omni:progress ?progress . " +
                "}";
        return executeListQuery(queryString);
    }

    // Method untuk Pending Tasks (Dibutuhkan TaskService)
    public List<Task> getPendingTasks(String employeeId) {
        String queryString = PREFIXES +
                "SELECT ?s ?title ?desc ?due ?status ?progress WHERE { " +
                "  ?s rdf:type omni:Task ; " +
                "     omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "     omni:title ?title ; " +
                "     omni:description ?desc ; " +
                "     omni:dueDate ?due ; " +
                "     omni:taskStatus ?status ; " +
                "     omni:progress ?progress . " +
                "  FILTER(?status != \"COMPLETED\") " +
                "}";
        return executeListQuery(queryString);
    }

    // Method untuk Today Tasks (Dibutuhkan TaskService)
    public List<Task> getTodayTasks(String employeeId) {
        String today = LocalDate.now().toString();
        String queryString = PREFIXES +
                "SELECT ?s ?title ?desc ?due ?status ?progress WHERE { " +
                "  ?s rdf:type omni:Task ; " +
                "     omni:hasEmployeeId \"" + employeeId + "\" ; " +
                "     omni:title ?title ; " +
                "     omni:description ?desc ; " +
                "     omni:dueDate ?due ; " +
                "     omni:taskStatus ?status ; " +
                "     omni:progress ?progress . " +
                "  FILTER(STR(?due) = \"" + today + "\") " +
                "}";
        return executeListQuery(queryString);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void executeUpdate(String sparql) {
        try (RDFConnection conn = RDFConfig.getConnection()) {
            conn.update(sparql);
            System.out.println("SPARQL Update Success");
        } catch (Exception e) {
            System.err.println("SPARQL Update Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Task> executeListQuery(String queryString) {
        List<Task> tasks = new ArrayList<>();
        try (RDFConnection conn = RDFConfig.getConnection();
             QueryExecution qExec = conn.query(queryString)) {

            ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Task t = new Task();
                String uri = soln.getResource("s").getURI();
                t.setId(uri.substring(uri.lastIndexOf("task_") + 5));
                t.setTitle(soln.getLiteral("title").getString());
                t.setDescription(soln.getLiteral("desc").getString());
                t.setDueDate(LocalDate.parse(soln.getLiteral("due").getString(), DATE_FMT));
                t.setStatus(TaskStatus.valueOf(soln.getLiteral("status").getString()));
                t.setProgressPercentage(soln.getLiteral("progress").getInt());
                tasks.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tasks;
    }
}