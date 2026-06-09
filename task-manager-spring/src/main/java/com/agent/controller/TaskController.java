package com.agent.controller;

import com.agent.model.Task;
import com.agent.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // 构造器注入：Spring 自动把 TaskService 的实例传进来
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // GET /api/tasks              → 获取所有任务
    // GET /api/tasks?status=DONE  → 获取已完成的任务
    @GetMapping
    public List<Task> list(@RequestParam(required = false) String status) {
        return taskService.listAll(status);
    }

    // GET /api/tasks/1   → 获取 id=1 的任务
    @GetMapping("/{id}")
    public Task getById(@PathVariable Long id) {
        return taskService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "任务不存在: " + id
                ));
    }

    // POST /api/tasks   → 创建新任务
    // @ResponseStatus(HttpStatus.CREATED) → 返回 201 而不是默认的 200
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestBody Task task) {
        return taskService.create(task);
    }

    // PUT /api/tasks/1   → 更新 id=1 的任务
    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody Task task) {
        return taskService.update(id, task)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "任务不存在: " + id
                ));
    }

    // DELETE /api/tasks/1   → 删除 id=1 的任务
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // 返回 204（无内容）
    public void delete(@PathVariable Long id) {
        boolean deleted = taskService.delete(id);
        if (!deleted) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "任务不存在: " + id
            );
        }
    }
}
