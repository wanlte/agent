package com.agent.controller;

import com.agent.dto.CreateTaskRequest;
import com.agent.dto.Priority;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 每个测试方法执行后自动回滚，保持数据库干净
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── 创建任务 ───

    @Test
    void shouldCreateTask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("测试任务");
        req.setPriority(Priority.HIGH);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("测试任务"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void shouldRejectEmptyTitle() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("");       // 空标题
        req.setPriority(Priority.MEDIUM);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectMissingPriority() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("无优先级");
        // 不设 priority

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── 查询 ───

    @Test
    void shouldReturnTaskById() throws Exception {
        // 先创建
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Spring Boot");
        req.setPriority(Priority.HIGH);

        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        int id = objectMapper.readTree(response).get("id").asInt();

        // 再查询
        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot"));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ─── 更新 ───

    @Test
    void shouldUpdateTask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("原始标题");
        req.setPriority(Priority.LOW);

        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        int id = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"新标题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("新标题"))
                .andExpect(jsonPath("$.priority").value("LOW")); // 未更新的字段不变
    }

    // ─── 删除 ───

    @Test
    void shouldDeleteTask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("待删除");
        req.setPriority(Priority.MEDIUM);

        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        int id = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isNotFound());
    }
}