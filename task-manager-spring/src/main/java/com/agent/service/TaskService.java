package com.agent.service;

import com.agent.model.Task;
import com.agent.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional   // 所有 public 方法默认在事务中执行
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)  // 只读事务，性能更好
    public List<Task> listAll(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return taskRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
        }
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Task> getById(Long id) {
        return taskRepository.findById(id);
    }

    public Task create(Task task) {
        return taskRepository.save(task);
    }

    public Optional<Task> update(Long id, Task updateData) {
        return taskRepository.findById(id).map(existing -> {
            if (updateData.getTitle() != null) existing.setTitle(updateData.getTitle());
            if (updateData.getDescription() != null) existing.setDescription(updateData.getDescription());
            if (updateData.getStatus() != null) existing.setStatus(updateData.getStatus());
            if (updateData.getPriority() != null) existing.setPriority(updateData.getPriority());
            return taskRepository.save(existing);  // updateData.createdAt 不会覆盖（@Column updatable=false）
        });
    }

    public boolean delete(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
}