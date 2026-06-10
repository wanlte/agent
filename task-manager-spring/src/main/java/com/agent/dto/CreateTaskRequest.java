package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在 2-100 之间")
    private String title;

    @Size(max = 500, message = "描述不能超过 500 字")
    private String description;

    @NotNull(message = "优先级不能为空")
    private Priority priority;   // 枚举：HIGH, MEDIUM, LOW

    // Getter / Setter
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}