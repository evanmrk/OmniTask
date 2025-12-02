package com.omnitask.service;

import com.omnitask.model.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TaskService {

    private SPARQLService sparqlService;

    public TaskService() {
        this.sparqlService = new SPARQLService();
    }


    public Task createTask(String employeeId, String title, String description, LocalDate dueDate) {
        Task task = new Task(
                UUID.randomUUID().toString(),
                employeeId,
                title,
                description,
                dueDate
        );

        sparqlService.saveTask(task);
        return task;
    }


    public void updateProgress(String taskId, int progress) throws Exception {
        Task task = sparqlService.getTask(taskId);
        if (task == null) {
            throw new Exception("Task not found");
        }

        task.setProgressPercentage(progress);

        // Auto-complete if progress is 100%
        if (progress >= 100) {
            task.setStatus(Task.TaskStatus.COMPLETED);
        }

        sparqlService.updateTask(task);
    }


    public void updateStatus(String taskId, Task.TaskStatus status) throws Exception {
        Task task = sparqlService.getTask(taskId);
        if (task == null) {
            throw new Exception("Task not found");
        }

        task.setStatus(status);
        sparqlService.updateTask(task);
    }


    public void addProofOfWork(String taskId, String proofPath) throws Exception {
        Task task = sparqlService.getTask(taskId);
        if (task == null) {
            throw new Exception("Task not found");
        }

        task.setProofOfWorkPath(proofPath);
        sparqlService.updateTask(task);
    }


    public void addDailyNotes(String taskId, String notes) throws Exception {
        Task task = sparqlService.getTask(taskId);
        if (task == null) {
            throw new Exception("Task not found");
        }

        String existingNotes = task.getDailyNotes();
        if (existingNotes != null && !existingNotes.isEmpty()) {
            notes = existingNotes + "\n---\n" + LocalDate.now() + ":\n" + notes;
        } else {
            notes = LocalDate.now() + ":\n" + notes;
        }

        task.setDailyNotes(notes);
        sparqlService.updateTask(task);
    }


    public List<Task> getTasksForEmployee(String employeeId) {
        return sparqlService.getTasksForEmployee(employeeId);
    }


    public List<Task> getTodayTasks(String employeeId) {
        return sparqlService.getTodayTasks(employeeId);
    }


    public List<Task> getPendingTasks(String employeeId) {
        return sparqlService.getPendingTasks(employeeId);
    }


    public void deleteTask(String taskId) {
        sparqlService.deleteTask(taskId);
    }
}