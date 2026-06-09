package com.agent.model;

import java.time.LocalDateTime;

public class Task {
    private Long id;
    private String title;
    private String description;
    private String status;       // TODO, IN_PROGRESS, DONE
    private String priority;     // HIGH, MEDIUM, LOW
    private LocalDateTime createdAt;

    // 无参构造（Jackson 反序列化 JSON → Java 对象时必须要有）
    public Task() {}

    // 全参构造（方便创建新任务）
    public Task(Long id, String title, String description, String status, String priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getter / Setter ───
    // Jackson 通过 getter 来序列化（对象→JSON），通过 setter 来反序列化（JSON→对象）

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}