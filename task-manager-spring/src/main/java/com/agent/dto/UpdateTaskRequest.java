package com.agent.dto;

import jakarta.validation.constraints.Size;

public class UpdateTaskRequest {

    @Size(min = 2, max = 100, message = "标题长度必须在 2-100 之间")
    private String title;

    @Size(max = 500, message = "描述不能超过 500 字")
    private String description;

    private String status;     // TODO, IN_PROGRESS, DONE
    private Priority priority;

    // Getter / Setter
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}