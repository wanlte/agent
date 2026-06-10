package com.agent.controller;

import com.agent.dto.CreateTaskRequest;
import com.agent.dto.TaskResponse;
import com.agent.dto.UpdateTaskRequest;
import com.agent.model.Task;
import com.agent.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) String status) {
        return taskService.listAll(status)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "任务不存在: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        // 把 Request DTO 转为 Model
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority().name());
        task.setStatus("TODO");

        Task created = taskService.create(task);
        return TaskResponse.from(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        Task updateData = new Task();
        updateData.setTitle(request.getTitle());
        updateData.setDescription(request.getDescription());
        updateData.setStatus(request.getStatus());
        if (request.getPriority() != null) {
            updateData.setPriority(request.getPriority().name());
        }

        return taskService.update(id, updateData)
                .map(TaskResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "任务不存在: " + id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!taskService.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在: " + id);
        }
    }
}