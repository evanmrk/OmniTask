package com.omnitask.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Import Baru
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;

import com.omnitask.config.RDFConfig;
import com.omnitask.model.Attendance;
import com.omnitask.model.Employee;
import com.omnitask.model.Location;
import com.omnitask.model.Task;
import com.omnitask.model.Task.TaskStatus;

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
    // EMPLOYEE METHODS (BARU: UNTUK LOGIN & REGISTRASI)
    // ==========================================

    public void saveEmployee(Employee emp) {
        // Mencegah error jika data null, ganti dengan string kosong
        String email = emp.getEmail() != null ? emp.getEmail() : "";
        String dept = emp.getDepartment() != null ? emp.getDepartment() : "";
        String role = emp.getRole() != null ? emp.getRole() : "";
        String target = emp.getDailyTarget() != null ? emp.getDailyTarget() : "-";
        String photo = emp.getPhotoPath() != null ? emp.getPhotoPath().replace("\\", "/") : "";

        String updateString = PREFIXES +
                "INSERT DATA { " +
                "  omni:emp_" + emp.getId() + " rdf:type omni:Employee ; " +
                "    omni:hasID \"" + emp.getId() + "\" ; " +
                "    omni:name \"" + emp.getName() + "\" ; " +
                "    omni:email \"" + email + "\" ; " +
                "    omni:department \"" + dept + "\" ; " +
                "    omni:role \"" + role + "\" ; " +
                "    omni:photoPath \"" + photo + "\" ; " + // Path foto fisik di komputer
                "    omni:dailyTarget \"" + target + "\" . " +
                "}";

        executeUpdate(updateString);
        System.out.println("✓ Employee " + emp.getName() + " saved to Fuseki.");
    }

    public Employee getEmployeeById(String id) {
        String queryString = PREFIXES +
                "SELECT ?name ?email ?dept ?role ?photo ?target WHERE { " +
                "  ?s rdf:type omni:Employee ; " +
                "     omni:hasID \"" + id + "\" ; " +
                "     omni:name ?name . " +
                "  OPTIONAL { ?s omni:email ?email } " +
                "  OPTIONAL { ?s omni:department ?dept } " +
                "  OPTIONAL { ?s omni:role ?role } " +
                "  OPTIONAL { ?s omni:photoPath ?photo } " +
                "  OPTIONAL { ?s omni:dailyTarget ?target } " +
                "} LIMIT 1";

        try (RDFConnection conn = RDFConfig.getConnection();
             QueryExecution qExec = conn.query(queryString)) {

            ResultSet rs = qExec.execSelect();
            if (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Employee emp = new Employee();
                emp.setId(id);
                emp.setName(soln.getLiteral("name").getString());
                
                if (soln.contains("email")) emp.setEmail(soln.getLiteral("email").getString());
                if (soln.contains("dept")) emp.setDepartment(soln.getLiteral("dept").getString());
                if (soln.contains("role")) emp.setRole(soln.getLiteral("role").getString());
                if (soln.contains("photo")) emp.setPhotoPath(soln.getLiteral("photo").getString());
                if (soln.contains("target")) emp.setDailyTarget(soln.getLiteral("target").getString());

                return emp;
            }
        } catch (Exception e) {
            System.err.println("Error fetching employee: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

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
        // Menggunakan omni namespace untuk URI
        String attendanceURI = NS + "att_" + attendance.getId(); 
        Resource attendanceRes = model.createResource(attendanceURI);

        Property rdfType = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Resource typeAttendance = model.createResource(NS + "Attendance");

        Property hasEmployeeId = model.createProperty(NS + "hasEmployeeId");
        Property checkInTime = model.createProperty(NS + "checkInTime");
        Property latitude = model.createProperty(NS + "latitude");
        Property longitude = model.createProperty(NS + "longitude");
        Property photoPath = model.createProperty(NS + "photoPath");
        Property status = model.createProperty(NS + "status");

        attendanceRes.addProperty(rdfType, typeAttendance);
        attendanceRes.addProperty(hasEmployeeId, attendance.getEmployeeId());
        attendanceRes.addProperty(checkInTime, attendance.getCheckInTime().format(DATETIME_FMT));
        attendanceRes.addProperty(latitude, String.valueOf(attendance.getLocation().getLatitude()));
        attendanceRes.addProperty(longitude, String.valueOf(attendance.getLocation().getLongitude()));
        attendanceRes.addProperty(photoPath, attendance.getPhotoPath());
        
        if(attendance.getStatus() != null) {
             attendanceRes.addProperty(status, attendance.getStatus());
        } else {
             attendanceRes.addProperty(status, "PRESENT");
        }

        conn.load(model);
        System.out.println("✓ Attendance saved to Fuseki");
    }

    public void updateCheckOut(String attendanceId, LocalDateTime checkOutTime) {
        String updateString = PREFIXES +
                "DELETE { ?s omni:checkOutTime ?old } " +
                "INSERT { ?s omni:checkOutTime \"" + checkOutTime.format(DATETIME_FMT) + "\" } " +
                "WHERE { " +
                "  BIND(omni:att_" + attendanceId + " AS ?s) " +
                "  OPTIONAL { ?s omni:checkOutTime ?old } " +
                "}";

        executeUpdate(updateString);
    }

    public List<Employee> getAllEmployees() {
        String queryString = PREFIXES +
                "SELECT ?id ?name ?role ?dept ?target WHERE { " +
                "  ?s rdf:type omni:Employee ; " +
                "     omni:hasID ?id ; " +
                "     omni:name ?name . " +
                "  OPTIONAL { ?s omni:role ?role } " +
                "  OPTIONAL { ?s omni:department ?dept } " +
                "  OPTIONAL { ?s omni:dailyTarget ?target } " +
                "}";

        List<Employee> list = new ArrayList<>();
        try (org.apache.jena.rdfconnection.RDFConnection conn = com.omnitask.config.RDFConfig.getConnection();
             org.apache.jena.query.QueryExecution qExec = conn.query(queryString)) {

            org.apache.jena.query.ResultSet rs = qExec.execSelect();
            while (rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                Employee emp = new Employee();
                emp.setId(soln.getLiteral("id").getString());
                emp.setName(soln.getLiteral("name").getString());
                if (soln.contains("role")) emp.setRole(soln.getLiteral("role").getString());
                if (soln.contains("dept")) emp.setDepartment(soln.getLiteral("dept").getString());
                if (soln.contains("target")) emp.setDailyTarget(soln.getLiteral("target").getString());
                list.add(emp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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
                
                double lat = Double.parseDouble(soln.getLiteral("lat").getString());
                double lon = Double.parseDouble(soln.getLiteral("lon").getString());
                att.setLocation(new Location(lat, lon));
                
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
    // TASK METHODS
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
                "                             omni:progress ?oldProg . " +
                "  omni:task_" + task.getId() + " omni:proofPath ?oldProof . " +
                "  omni:task_" + task.getId() + " omni:notes ?oldNotes . " +
                "} INSERT { " +
                "  omni:task_" + task.getId() + " omni:taskStatus \"" + task.getStatus().name() + "\" ; " +
                "                             omni:progress \"" + task.getProgressPercentage() + "\"^^xsd:int . " +
                (task.getProofOfWorkPath() != null ? "  omni:task_" + task.getId() + " omni:proofPath \"" + task.getProofOfWorkPath() + "\" . " : "") +
                (task.getDailyNotes() != null ? "  omni:task_" + task.getId() + " omni:notes \"" + task.getDailyNotes() + "\" . " : "") +
                "} WHERE { " +
                "  omni:task_" + task.getId() + " omni:taskStatus ?oldStatus ; " +
                "                             omni:progress ?oldProg . " +
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
                t.setEmployeeId(soln.getLiteral("empId") != null ? soln.getLiteral("empId").getString() : "");
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