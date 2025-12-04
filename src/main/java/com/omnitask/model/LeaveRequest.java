package com.omnitask.model;

import java.time.LocalDate;

public class LeaveRequest {
    private String requestId;
    private String employeeId;
    private String employeeName;
    private RequestType type;       // CUTI, IZIN, DINAS
    private RequestStatus status;   // PENDING, APPROVED, REJECTED

    // Field untuk Cuti/Izin Biasa
    private LocalDate startDate;
    private String reason;
    private String duration;

    // Field Khusus Dinas
    private LocalDate endDate;
    private String destination;
    private String transport;

    // Enum Tipe Izin
    public enum RequestType {
        CUTI, IZIN, DINAS
    }

    // Enum Status Izin
    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    public LeaveRequest() {}

    // --- GETTERS & SETTERS ---

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
}