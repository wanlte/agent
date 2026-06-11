package com.agent.repository;

import com.agent.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 按状态查询（Spring Data JPA 解析方法名自动生成 SQL）
    List<Task> findByStatus(String status);

    // 按状态 + 创建时间倒序
    List<Task> findByStatusOrderByCreatedAtDesc(String status);

    // 按优先级 + 创建时间倒序
    List<Task> findByPriorityOrderByCreatedAtDesc(String priority);
}