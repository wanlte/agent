package com.agent.service;

import com.agent.model.Task;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service  // ← 告诉 Spring：这是一个 Service，请创建它的实例
public class TaskService {

    // 用 ConcurrentHashMap 做内存存储（线程安全，重启就没了，Day 4 换数据库）
    private final Map<Long, Task> taskMap = new ConcurrentHashMap<>();

    // 自增 ID 生成器（线程安全）
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 获取所有任务，支持按状态过滤，按创建时间倒序
     */
    public List<Task> listAll(String statusFilter) {
        List<Task> all = new ArrayList<>(taskMap.values());

        // 如果传了 status 过滤条件，就筛选
        if (statusFilter != null && !statusFilter.isBlank()) {
            all = all.stream()
                     .filter(t -> t.getStatus().equalsIgnoreCase(statusFilter))
                     .toList();
        }

        // 按创建时间倒序（最新的在前面）
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return all;
    }

    /**
     * 根据 ID 获取单个任务。找不到返回 Optional.empty()
     */
    public Optional<Task> getById(Long id) {
        return Optional.ofNullable(taskMap.get(id));
    }

    /**
     * 创建新任务。自动生成 ID、创建时间、默认状态和优先级
     */
    public Task create(Task task) {
        task.setId(idGenerator.getAndIncrement());
        task.setCreatedAt(java.time.LocalDateTime.now());
        if (task.getStatus() == null) {
            task.setStatus("TODO");
        }
        if (task.getPriority() == null) {
            task.setPriority("MEDIUM");
        }
        taskMap.put(task.getId(), task);
        return task;
    }

    /**
     * 更新任务。只更新传了值的字段（部分更新）
     */
    public Optional<Task> update(Long id, Task updateData) {
        Task existing = taskMap.get(id);
        if (existing == null) {
            return Optional.empty();
        }

        // 只更新不为 null 的字段（部分更新）
        if (updateData.getTitle() != null) existing.setTitle(updateData.getTitle());
        if (updateData.getDescription() != null) existing.setDescription(updateData.getDescription());
        if (updateData.getStatus() != null) existing.setStatus(updateData.getStatus());
        if (updateData.getPriority() != null) existing.setPriority(updateData.getPriority());

        return Optional.of(existing);
    }

    /**
     * 删除任务。返回是否成功
     */
    public boolean delete(Long id) {
        return taskMap.remove(id) != null;
    }
}