# Phase 1 Week 1: Spring Boot 速通 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零搭建一个完整的 Spring Boot Task Manager API，能 CRUD、有校验、数据持久化、可打包部署

**Architecture:** 经典三层架构（Controller → Service → Repository），内存存储 → JPA/H2 持久化渐进演进

**Tech Stack:** Java 17+, Spring Boot 3.x, Maven, H2 Database, JPA, MockMvc

---

## Day 1: Spring Boot 从零到 Hello World

### 教学模式

每节课按以下流程进行：
1. **讲原理** — 我会先解释这步涉及的核心概念（10-15 分钟阅读）
2. **给任务** — 我给出具体的代码和操作指令
3. **你执行** — 你在本地敲代码、运行
4. **我检查** — 你把运行结果/截图/报错发给我，我帮你排查

---

### Task 1.1: 环境准备

**需要安装的工具：**
- JDK 17 或 21（`java -version` 确认）
- Maven 3.8+（`mvn -version` 确认）
- IntelliJ IDEA Community（推荐）或 VS Code

**你执行：**

- [ ] **Step 1: 确认 Java 版本**

```bash
java -version
```

期望看到: `openjdk version "17.x"` 或 `"21.x"`

- [ ] **Step 2: 确认 Maven 版本**

```bash
mvn -version
```

期望看到: `Apache Maven 3.8.x` 或更高

告诉我你是否已经安装了这些工具，如果没有，我会帮你安装。

---

### Task 1.2: 理解 Spring Boot 的本质

**在动手之前，先理解 3 件事：**

#### 1. 什么是 Spring Boot 应用

一个 Spring Boot 应用就是一个**普通的 Java 程序**，只不过它自带了一个嵌入的 Web 服务器（Tomcat）。你运行 `main` 方法，Tomcat 就启动了，然后它一直运行，监听端口等请求。

```java
// 就这么一个 main 方法，启动后：
// 1. Spring 扫描你所有的类，找到 @RestController、@Service 等注解
// 2. 自动创建这些类的实例（这叫"依赖注入"）
// 3. 启动嵌入的 Tomcat，监听 8080 端口
// 4. 当有 HTTP 请求进来，Tomcat 把请求转给对应的 Controller 方法
public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
}
```

#### 2. 什么是 Maven

Maven 做两件事：
- **管理依赖**：你在 `pom.xml` 里声明"我需要 Spring Boot Web"，Maven 自动下载这个 jar 以及它依赖的所有 jar（不需要你手动找 jar 包）
- **构建项目**：`mvn package` 把代码编译成 `.class`、打包成 `.jar`

#### 3. 一个最小的 Spring Boot 项目只需要 3 个文件

不是 30 个，不是 300 个，是 **3 个**：

```
task-manager-spring/
├── pom.xml                        # 告诉 Maven：我需要什么依赖
├── src/main/java/com/agent/
│   └── Application.java           # 启动类（main 方法）
└── src/main/resources/
    └── application.yml            # 配置文件（端口、数据库等）
```

---

### Task 1.3: 创建项目结构

**你执行：**

- [ ] **Step 1: 创建项目目录**

```bash
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/resources
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/test/java/com/agent
```

- [ ] **Step 2: 创建 pom.xml**

在 `F:/java.work/learning.txt/agent/task-manager-spring/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标：groupId.artifactId 唯一标识一个项目 -->
    <groupId>com.agent</groupId>
    <artifactId>task-manager-spring</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <!-- 继承 Spring Boot 父 POM：锁定所有 Spring 依赖的版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- spring-boot-starter-web: 包含 Spring MVC + 嵌入 Tomcat + Jackson JSON -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- spring-boot-starter-test: 包含 JUnit 5 + MockMvc + Mockito -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**关键点理解**：
- `spring-boot-starter-web` 不是一个 jar，是一组 jar 的集合（Tomcat + Spring MVC + Jackson + ...）。这就是 "starter" 的含义——一个依赖带一群
- `spring-boot-starter-parent` 锁定了 Spring 生态所有库的版本号，你不用手动指定版本，避免版本冲突
- `<packaging>jar</packaging>` 表示最终打包成一个可执行的 jar（里面包含嵌入的 Tomcat）

- [ ] **Step 3: 创建启动类**

在 `F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/Application.java`：

```java
package com.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**@SpringBootApplication 的三个作用**（拆开看）：

```java
// @SpringBootApplication = 这三个注解的合体：

@Configuration        // 1. 这个类可以作为配置类，里面可以定义 Bean
@EnableAutoConfiguration  // 2. 自动配置：看到 classpath 里有 spring-web，就自动配好 Tomcat
@ComponentScan       // 3. 扫描当前包及子包下所有 @Component @Service @Controller
```

- [ ] **Step 4: 创建配置文件**

在 `F:/java.work/learning.txt/agent/task-manager-spring/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: task-manager
```

- [ ] **Step 5: 创建 Hello World Controller**

在 `F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/controller/HelloController.java`：

```java
package com.agent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello Agent World");
    }
}
```

**逐行解释**：

```java
@RestController  
// = @Controller + @ResponseBody
// 告诉 Spring：这个类处理 HTTP 请求，方法的返回值直接序列化为 JSON

public class HelloController {

    @GetMapping("/hello")  
    // 当有人发 GET 请求到 /hello 时，调用这个方法
    // @GetMapping = @RequestMapping(method = GET) 的简写

    public Map<String, String> hello() {
        // 返回值 Map 会被 Jackson 自动转成 JSON：
        // {"message": "Hello Agent World"}
        return Map.of("message", "Hello Agent World");
    }
}
```

- [ ] **Step 6: 运行项目**

```bash
cd F:/java.work/learning.txt/agent/task-manager-spring
mvn spring-boot:run
```

第一次运行会下载依赖（可能需要几分钟），看到以下日志说明成功：

```
Started Application in 2.5 seconds
```

- [ ] **Step 7: 访问 API**

浏览器打开 `http://localhost:8080/hello`

期望看到：
```json
{"message": "Hello Agent World"}
```

---

### Day 1 核心要点总结

1. Spring Boot 应用 = `main` 方法 + 内嵌 Tomcat，跑起来就是一个 Web 服务器
2. `pom.xml` 声明依赖，Maven 自动下载，不用手动管 jar 包
3. `@SpringBootApplication` 做三件事：配置 + 自动配置 + 组件扫描
4. `@RestController` + `@GetMapping` 定义 HTTP API
5. 注意项目结构：你的类必须在 `Application.java` 所在包的**同级或子级**，否则扫描不到

---

### Task 1.4: 验证理解

请回答以下问题（发给我检查）：

1. `@RestController` 和 `@Controller` 有什么区别？（提示：想想 JSON 返回 vs 页面返回）
2. 如果把 `HelloController` 移到 `com.other` 包下面，会发生什么？为什么？
3. `spring-boot-starter-web` 包含哪些东西？如果不用 starter，你需要手动声明哪些依赖？

---

**执行完成后**，把以下内容发给我：
- 项目目录结构截图（或 `tree` 命令输出）
- `http://localhost:8080/hello` 返回结果截图
- 上面 3 个问题的回答

---

---

---

## Day 2: RESTful CRUD API

### 讲原理

**REST 是什么？** REST 不是协议，是一套**约定**——URL 代表资源（名词），HTTP 方法代表操作（动词）。

```
❌ 不 REST：
POST /createTask
GET /getTask?id=1
POST /updateTask
GET /deleteTask?id=1

✅ REST：
POST   /api/tasks       → 创建任务
GET    /api/tasks       → 获取任务列表
GET    /api/tasks/1     → 获取单个任务
PUT    /api/tasks/1     → 更新任务
DELETE /api/tasks/1     → 删除任务
```

**Spring Boot 如何把 HTTP 请求映射到 Java 方法**：

```
HTTP 请求                               Java 方法
─────────────────────────────────────────────────────────────
GET  /api/tasks           →    @GetMapping            →  list()
GET  /api/tasks/1         →    @GetMapping("/{id}")   →  getById(1)
POST /api/tasks           →    @PostMapping           →  create(json_body)
PUT  /api/tasks/1         →    @PutMapping("/{id}")   →  update(1, json_body)
DELETE /api/tasks/1       →    @DeleteMapping("/{id}")→  delete(1)
```

**三个参数来源注解**：

| 注解 | 取值的来源 | 示例 |
|------|-----------|------|
| `@PathVariable` | URL 路径中 | `/api/tasks/{id}` → `id=1` |
| `@RequestBody` | HTTP 请求体（JSON） | `{"title":"学习"}` → Java 对象 |
| `@RequestParam` | URL 问号后面 | `/api/tasks?status=DONE` → `status="DONE"` |

**三层架构**：

```
HTTP 请求
  ↓
Controller  ← 接收请求、参数校验、返回响应（不写业务逻辑）
  ↓
Service     ← 业务逻辑（创建任务、过滤列表...）
  ↓
Repository  ← 数据存取（今天先用内存 Map 代替，Day 4 再换数据库）
```

**关键注解**：
- `@Service`：标记业务层组件，Spring 自动创建实例
- `@RestController`：标记控制器
- `@RequestMapping("/api/tasks")`：类级别路径前缀
- 构造器注入：`public TaskController(TaskService ts) { this.ts = ts; }` — Spring 自动把 Service 实例传进来

---

### Task 2.1: 创建目录结构

**你执行：**

```bash
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/model
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/service
```

---

### Task 2.2: 创建 Task 模型类

**文件**: `src/main/java/com/agent/model/Task.java`

```java
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
```

**为什么写这么多 Getter/Setter？**

Jackson（Spring Boot 自带的 JSON 库）依赖它们工作：
- **序列化**（Java 对象 → JSON 字符串）：Jackson 找 `getXxx()` 方法，生成 `"xxx": 值`
- **反序列化**（JSON 字符串 → Java 对象）：Jackson 找 `setXxx()` 方法，把 JSON 字段值设进去

没有 getter/setter，Jackson 无法转换。

---

### Task 2.3: 创建 TaskService 业务层

**文件**: `src/main/java/com/agent/service/TaskService.java`

```java
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
```

**关键概念**：

| 代码 | 为什么这样写 |
|------|-------------|
| `@Service` | 告诉 Spring 自动创建这个类的实例，其他地方需要时自动注入 |
| `ConcurrentHashMap` | 普通 HashMap 在多线程下不安全，ConcurrentHashMap 是线程安全版本 |
| `AtomicLong` | 线程安全的自增 ID，`getAndIncrement()` = 先取值再 +1 |
| `Optional<Task>` | 明确表示"可能没有返回值"，调用方必须处理空的情况 |
| `stream().filter()` | Java 8 Stream API，函数式风格过滤数据 |

---

### Task 2.4: 创建 TaskController 控制器

**文件**: `src/main/java/com/agent/controller/TaskController.java`

```java
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
```

**Controller 层的职责边界**：
- ✅ 接收 HTTP 请求、提取参数
- ✅ 调用 Service 处理业务
- ✅ 返回响应 + 正确的 HTTP 状态码
- ❌ 不写业务逻辑（那是 Service 的事）
- ❌ 不直接操作数据（那是 Repository 的事）

**HTTP 状态码速记**：

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 OK | 成功 | GET、PUT 成功 |
| 201 Created | 已创建 | POST 成功 |
| 204 No Content | 成功但无返回体 | DELETE 成功 |
| 404 Not Found | 资源不存在 | ID 找不到 |

---

### Task 2.5: 删除 HelloController（可选）

如果保留也不影响，但建议删掉保持项目干净：

```bash
rm F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/controller/HelloController.java
```

---

### Task 2.6: 启动并测试

**Step 1**: 启动项目

```bash
cd F:/java.work/learning.txt/agent/task-manager-spring
mvn spring-boot:run
```

看到 `Started Application in X seconds` 后，新开一个终端窗口。

**Step 2**: 用 curl 测试所有 API

```bash
# 1. 创建一个任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"学习 Spring Boot","description":"完成 Day2 CRUD 练习","priority":"HIGH"}'

# 2. 再创建两个任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"写单元测试","description":"用 JUnit 写测试","priority":"MEDIUM","status":"IN_PROGRESS"}'

curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"部署到服务器","description":"打包 Docker 镜像","status":"TODO"}'

# 3. 获取所有任务
curl http://localhost:8080/api/tasks

# 4. 按状态过滤（只要 TODO 的）
curl "http://localhost:8080/api/tasks?status=TODO"

# 5. 获取单个任务（把 1 换成实际 ID）
curl http://localhost:8080/api/tasks/1

# 6. 更新任务（部分更新：只改 status，不改其他字段）
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"DONE"}'

# 7. 确认更新成功
curl http://localhost:8080/api/tasks/1

# 8. 删除任务
curl -X DELETE http://localhost:8080/api/tasks/1

# 9. 确认删除成功（应该返回 404）
curl http://localhost:8080/api/tasks/1

# 10. 验证参数校验还没生效（Day 3 会加）——创建空任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{}'
```

**注意**：第 10 步会创建一个所有字段都是 null 的任务。这说明输入校验还没到位——这就是 Day 3 要解决的问题。

---

### Day 2 核心要点总结

1. **REST 是约定**：URL = 名词（资源），HTTP 方法 = 动词（操作）
2. **三层架构**：Controller（接收）→ Service（处理）→ Repository（存储）
3. **三个参数注解**：`@PathVariable`（路径）、`@RequestBody`（请求体 JSON）、`@RequestParam`（URL 参数）
4. **构造器注入**优于 `@Autowired` 字段注入——依赖明确、方便测试
5. **`Optional`** 明确表达"可能没有值"，避免 NPE
6. **HTTP 状态码**要正确：201 创建、204 删除、404 找不到

---

### Task 2.7: 验证理解

请回答以下问题：

1. Controller 里为什么用构造器传入 `TaskService`，而不是 `new TaskService()`？
2. `@RequestParam(required = false)` 是什么意思？如果去掉 `required = false`，访问 `/api/tasks`（不带 ?status=）会发生什么？
3. 创建任务时，前端传的 JSON 是怎么变成 Java Task 对象的？谁做的转换？
4. 如果访问 `GET /api/tasks/999`（不存在的 ID），Controller 会返回什么？状态码是多少？

---

**执行完成后**，把以下内容发给我：
- `GET /api/tasks` 和 `POST /api/tasks` 的输出结果
- 上面 4 个问题的回答

---

---

---

## Day 3: 参数校验 + 全局异常处理

### 讲原理

**为什么需要校验？**

目前创建任务时可以传 `{}`（空对象），得到一个所有字段都是 null 的"垃圾任务"。原因：**你没有验证用户输入**。

**信任边界**：程序内部的数据你可以信任，但从外部（HTTP 请求）进来的数据**完全不可信**。校验必须在 Controller 层——数据一进门就要检查。

**校验分两层**：

```
HTTP 请求
  ↓
Controller  → 第一道防线：格式校验（标题不能为空、优先级必须是 HIGH/MEDIUM/LOW）
  ↓
Service     → 第二道防线：业务校验（不能创建同名的任务...今天不涉及）
```

**Spring Boot 校验体系**：

```
1. 在请求体 DTO 上加注解 → @NotBlank, @Size, @NotNull...
2. 在 Controller 参数上加 @Valid → 告诉 Spring "这个参数需要校验"
3. 如果校验不通过 → Spring 抛出 MethodArgumentNotValidException
4. @ControllerAdvice 全局拦截这个异常 → 返回统一格式的错误响应
```

**两种 DTO 的设计理念**：

```java
// ❌ 用一个类同时做请求体和响应体（Day 2 的做法）
// 问题：请求时 id 不应该由用户传，响应时应该有 createdAt

// ✅ CreateTaskRequest：只包含创建时需要用户提供的字段
public class CreateTaskRequest {
    @NotBlank String title;
    @Size(max=500) String description;
    @NotNull Priority priority;
}

// ✅ TaskResponse：包含返回给用户的所有字段
public class TaskResponse {
    Long id;
    String title;
    String description;
    String status;
    String priority;
    LocalDateTime createdAt;
}
```

**为什么要统一错误响应格式？**

```
❌ 不统一：
校验失败 → 400 + Spring 默认格式（字段名、错误信息混在一起）
ID 不存在 → 404 + 另一套格式
服务器报错 → 500 + 又一套格式

✅ 统一后：
{
  "code": 400,
  "message": "输入校验失败",
  "details": [
    {"field": "title", "message": "任务标题不能为空"}
  ],
  "timestamp": "2026-06-09T10:30:00"
}

前端/Agent/调用方只需要处理这一种错误结构。
```

---

### Task 3.1: 添加校验依赖

**文件**: `pom.xml`，在 `</dependencies>` 前添加：

```xml
        <!-- Bean Validation：@NotBlank、@Size、@NotNull 等注解 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

> Spring Boot 3.x 之后，`spring-boot-starter-web` 不再自动包含 validation，需要单独引入。

---

### Task 3.2: 创建请求/响应 DTO + 统一错误响应类

**Step 1**: 创建目录

```bash
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/dto
mkdir -p F:/java.work/learning.txt/agent/task-manager-spring/src/main/java/com/agent/exception
```

**Step 2**: 创建 `src/main/java/com/agent/dto/CreateTaskRequest.java`

```java
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
```

**Step 3**: 创建 `src/main/java/com/agent/dto/Priority.java`

```java
package com.agent.dto;

public enum Priority {
    HIGH, MEDIUM, LOW
}
```

**Step 4**: 创建 `src/main/java/com/agent/dto/UpdateTaskRequest.java`

```java
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
```

**Step 5**: 创建 `src/main/java/com/agent/dto/TaskResponse.java`

```java
package com.agent.dto;

import com.agent.model.Task;
import java.time.LocalDateTime;

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime createdAt;

    // 静态工厂方法：从 Model 转为 Response
    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.status = task.getStatus();
        r.priority = task.getPriority();
        r.createdAt = task.getCreatedAt();
        return r;
    }

    // Getter（响应不需要 Setter，由 from() 构建）
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

**Step 6**: 创建 `src/main/java/com/agent/exception/ErrorResponse.java`

```java
package com.agent.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {
    private int code;
    private String message;
    private List<FieldError> details;
    private LocalDateTime timestamp;

    public ErrorResponse(int code, String message, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getter
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldError> getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // 内部类：单个字段错误
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}
```

---

### Task 3.3: 创建全局异常处理器

**文件**: `src/main/java/com/agent/exception/GlobalExceptionHandler.java`

```java
package com.agent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice   // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // 1. 捕获校验失败异常（@Valid 不通过时抛出）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        return new ErrorResponse(400, "输入校验失败", details);
    }

    // 2. 捕获 ResponseStatusException（我们手动抛的 404 等）
    //    用 ResponseEntity 动态设置 HTTP 状态码（因为可能是 404/400/500，不固定）
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse body = new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getReason(),
            null
        );
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    // 3. 兜底：捕获所有其他异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex) {
        return new ErrorResponse(500, "服务器内部错误: " + ex.getMessage(), null);
    }
}
```

**`@RestControllerAdvice` 本质**：AOP（面向切面编程）。它在所有 Controller 外面包了一层拦截器——Controller 抛出的异常先经过这里，转成统一格式再返回给客户端。

---

### Task 3.4: 改造 TaskController

把 Controller 改为使用新的 DTO：

```java
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
        // 没传的字段显式设为 null，避免 Task 默认值（"MEDIUM"）干扰部分更新
        updateData.setPriority(request.getPriority() != null ? request.getPriority().name() : null);

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
```

**核心改动**：
1. `@RequestBody CreateTaskRequest` 替换 `@RequestBody Task` — 请求用专用 DTO
2. `@Valid` 加在参数前 — 触发校验
3. 返回 `TaskResponse` 而不是 `Task` — 响应用专用 DTO
4. Controller 里写 `TaskResponse.from(task)` 做类型转换 — Model 永远不直接暴露给外部

---

### Task 3.5: 测试

```powershell
# 1. 正常创建（应该成功 201）
Invoke-WebRequest -Uri http://localhost:8080/api/tasks `
  -Method Post -ContentType "application/json" `
  -Body '{"title":"学习Spring Boot","description":"完成Day3","priority":"HIGH"}'

# 2. 标题太短（应该返回 400 + 错误详情）
Invoke-WebRequest -Uri http://localhost:8080/api/tasks `
  -Method Post -ContentType "application/json" `
  -Body '{"title":"学","priority":"HIGH"}'

# 3. 缺少 priority（应该返回 400）
Invoke-WebRequest -Uri http://localhost:8080/api/tasks `
  -Method Post -ContentType "application/json" `
  -Body '{"title":"测试任务"}'

# 4. 查询不存在的 ID（应该返回 404 + 统一错误格式）
Invoke-WebRequest -Uri http://localhost:8080/api/tasks/99999

# 5. 正常查询列表
Invoke-WebRequest -Uri http://localhost:8080/api/tasks | Select-Object -ExpandProperty Content
```

---

### Day 3 核心要点总结

1. **信任边界**：外部输入不可信，校验必须在 Controller 层
2. **DTO 分离**：请求体（CreateTaskRequest）≠ 响应体（TaskResponse）≠ 领域模型（Task）
3. **@Valid + @NotBlank/@NotNull/@Size**：声明式校验，不写 if-else
4. **@RestControllerAdvice**：全局拦截异常，返回统一格式（前端/Agent 只需要处理一种错误结构）
5. **校验注解**来自 `jakarta.validation`（不是 `javax.validation`，Spring Boot 3.x 改了包名）

---

### Task 3.6: 验证理解

1. 为什么需要 CreateTaskRequest 和 TaskResponse 两个类，而不是直接复用 Task？
2. `@Valid` 放在 Controller 方法的参数上，背后发生了什么？
3. 如果 Controller 里不写 `@Valid`，只写校验注解在 DTO 上，会生效吗？
4. `@RestControllerAdvice` 比每个方法里写 try-catch 好在哪里？

---

---

---

## Day 4: JPA + H2 数据库持久化

### 讲原理

**当前状态（Day 3）**：数据存在 `ConcurrentHashMap` 里。重启项目 → 数据全没。

**目标状态（Day 4）**：数据存到数据库。重启项目 → 数据还在。

**ORM 是什么**：Object-Relational Mapping（对象关系映射）。你不用写 SQL，用操作 Java 对象的方式操作数据库。

```
// 不用 ORM（原生 JDBC）：
Connection conn = ...;
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tasks WHERE id = ?");
stmt.setLong(1, id);
ResultSet rs = stmt.executeQuery();
Task task = new Task();
task.setId(rs.getLong("id"));
task.setTitle(rs.getString("title"));
// ... 20 行代码才查出 1 条记录

// 用 JPA（ORM）：
Task task = taskRepository.findById(id).orElseThrow();
// 1 行搞定
```

**JPA 核心注解**：

| 注解 | 作用 | 放在 |
|------|------|------|
| `@Entity` | 标记这个类是数据库表对应的实体 | 类上 |
| `@Table(name="xxx")` | 指定表名（不写则默认类名小写） | 类上 |
| `@Id` | 主键 | 字段上 |
| `@GeneratedValue` | 主键自增策略 | 字段上 |
| `@Column` | 指定列名、长度、是否可空 | 字段上 |
| `@Enumerated(STRING)` | 把枚举存为字符串 | 枚举字段上 |

**H2 数据库**：纯 Java 的嵌入式数据库，存在内存或文件里。开发时零安装、零配置。默认自带一个 Web 控制台（`/h2-console`），可以像 Navicat 一样可视化查看数据。

**Spring Data JPA 魔法**：

```java
// 你只需要写一个接口，Spring 自动生成实现类
public interface TaskRepository extends JpaRepository<Task, Long> {
    // 继承 JpaRepository 后，自动获得：
    // findAll()     → SELECT * FROM tasks
    // findById(id)  → SELECT * FROM tasks WHERE id = ?
    // save(task)    → INSERT 或 UPDATE
    // deleteById(id)→ DELETE FROM tasks WHERE id = ?
    // count()       → SELECT COUNT(*) FROM tasks

    // 你还可以按命名规则自定义查询方法，不用写 SQL
    List<Task> findByStatus(String status);               // WHERE status = ?
    List<Task> findByPriorityOrderByCreatedAtDesc(Priority p);  // WHERE priority = ? ORDER BY created_at DESC
}
```

**事务（@Transactional）**：保证多个数据库操作要么全成功、要么全失败。银行转账：A 扣钱 + B 加钱，如果中间断电，两个操作都回滚，不会出现钱消失的情况。

---

### Task 4.1: 添加依赖

在 `pom.xml` 的 `</dependencies>` 前添加：

```xml
        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 数据库（开发环境，零安装） -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
```

---

### Task 4.2: 配置数据库

在 `application.yml` 中替换/追加：

```yaml
spring:
  application:
    name: task-manager

  # H2 数据库配置
  datasource:
    url: jdbc:h2:file:./data/taskdb   # 文件模式，数据存磁盘，重启不丢失
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # H2 Web 控制台（开发时可浏览器访问 /h2-console 查看数据库）
  h2:
    console:
      enabled: true
      path: /h2-console

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: update   # 自动建表/更新表结构（开发用，生产要改成 validate）
    show-sql: true       # 控制台打印 SQL，方便学习
    properties:
      hibernate:
        format_sql: true # SQL 格式化输出
```

**`ddl-auto` 的四个值**：

| 值 | 行为 | 适用场景 |
|----|------|---------|
| `update` | 自动建表，实体变了自动更新 | 开发阶段 |
| `create-drop` | 启动时建表，停时删表 | 测试 |
| `create` | 启动时建表（会删旧表！） | 全新项目 |
| `validate` | 只校验表结构是否匹配，不改 | 生产环境 |

---

### Task 4.3: 改造 Task 实体类

**文件**: `src/main/java/com/agent/model/Task.java`

```java
package com.agent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String status = "TODO";    // TODO, IN_PROGRESS, DONE

    @Column(nullable = false)
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // JPA 要求有无参构造
    public Task() {}

    // 创建时自动设时间
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getter / Setter
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
```

**改动说明**：
- 加了 `@Entity` `@Table` `@Id` `@GeneratedValue` 等 JPA 注解
- 用 `@PrePersist` 替代 Service 里的手动设时间——**让实体自己管理自己的生命周期**
- `updatable = false` 防止创建时间被意外修改
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` 让数据库自动生成 ID

---

### Task 4.4: 创建 Repository

**文件**: `src/main/java/com/agent/repository/TaskRepository.java`

```java
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
```

**你只写了一个接口（interface），不写实现类。Spring Data JPA 在运行时自动生成实现类。** 方法名 `findByStatusOrderByCreatedAtDesc` 被解析为：

```sql
SELECT * FROM tasks WHERE status = ? ORDER BY created_at DESC
```

---

### Task 4.5: 改造 TaskService

```java
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
```

**改动说明**：
- `ConcurrentHashMap` + `AtomicLong` 全部删掉，换成 `TaskRepository`
- `create()` 从 8 行变成 1 行——`taskRepository.save()`
- Service 不再手动生成 ID 和时间——JPA 自动处理
- `@Transactional(readOnly = true)` 标记读操作为只读事务

---

### Task 4.6: 启动并测试

```bash
mvn spring-boot:run
```

启动后：
1. 浏览器打开 `http://localhost:8080/h2-console`
2. JDBC URL 填 `jdbc:h2:file:./data/taskdb`，Connect
3. 你应该能看到 `TASKS` 表（JPA 自动建的）

测试持久化：

```powershell
# 1. 创建任务
Invoke-WebRequest -Uri http://localhost:8080/api/tasks `
  -Method Post -ContentType "application/json" `
  -Body '{"title":"持久化测试","description":"重启后应该还在","priority":"HIGH"}'

# 2. 查列表
Invoke-WebRequest -Uri http://localhost:8080/api/tasks | Select-Object -ExpandProperty Content

# 3. 重启项目（Ctrl+C 再 mvn spring-boot:run）

# 4. 再查列表——之前创建的任务应该还在！
Invoke-WebRequest -Uri http://localhost:8080/api/tasks | Select-Object -ExpandProperty Content
```

---

### Day 4 核心要点总结

1. **JPA 让你用操作对象的方式操作数据库**，不用写 SQL
2. **Spring Data JPA 自动生成实现类**——你只写 `interface`，框架写实现
3. **H2 是纯 Java 嵌入式数据库**，零安装，开发神器
4. **`ddl-auto: update`** 自动同步实体和表结构
5. **`@Transactional`** 保证数据一致性
6. **Repository 方法名即查询**：`findByStatusOrderByCreatedAtDesc` → SQL

---

### Task 4.7: 验证理解

1. `@GeneratedValue(strategy = GenerationType.IDENTITY)` 是什么意思？谁负责生成 ID？
2. `ddl-auto: update` 和 `ddl-auto: create` 有什么区别？生产环境应该用什么？
3. `@Transactional(readOnly = true)` 加和不加有什么区别？
4. JpaRepository 你只写了接口，谁在运行时提供了实现？

---

---

---

## Day 5: 集成测试 + 打包部署

### 讲原理

**单元测试 vs 集成测试**：

```
单元测试：测一个类/方法（把依赖 mock 掉）
集成测试：测整个系统（从 HTTP 请求到数据库，全链路）
```

**MockMvc**：Spring Boot 提供的不需要启动真实 HTTP 服务器的测试工具。它模拟 HTTP 请求，走完整的 Spring MVC 链路（Filter → Interceptor → Controller → Service → Repository → 数据库），但不占用真实端口。

**测试金字塔**：

```
        /\
       /E2E\        ← 少：端到端测试（启动整个系统）
      /------\
     /集成测试\     ← 中：MockMvc 测试 API
    /----------\
   /  单元测试  \   ← 多：测单个方法
  /--------------\
```

---

### Task 5.1: 写集成测试

**文件**: `src/test/java/com/agent/controller/TaskControllerTest.java`

```java
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
```

**测试文件的关键注解**：
- `@SpringBootTest`：启动完整的 Spring 上下文（包括数据库）
- `@AutoConfigureMockMvc`：自动配置 MockMvc
- `@BeforeEach`：每个测试方法前执行，保证测试独立性

---

### Task 5.2: 运行测试

```bash
cd F:/java.work/learning.txt/agent/task-manager-spring
mvn test
```

期望看到：`Tests run: 7, Failures: 0, Errors: 0`

---

### Task 5.3: 打包部署

```bash
# 打包（含测试，@Transactional 保证测试数据自动回滚，无需额外清理）
mvn package

# 运行
java -jar target/task-manager-spring-1.0.0.jar
```

打包成功后，浏览器验证 `http://localhost:8080/api/tasks`。

---

### Day 5 核心要点总结

1. **MockMvc** 不走真实 HTTP 端口，但走完整的 Spring MVC 链路
2. `@SpringBootTest` 启动完整上下文（含数据库），是集成测试
3. `jsonPath("$.field").value("expected")` — 链式断言 JSON 返回值
4. `mvn package` 产出可执行 jar（内嵌 Tomcat + 依赖 + 代码）
5. `java -jar xxxx.jar` 一条命令部署，不需要安装 Tomcat

---

### Day 5: 验证理解

1. `@SpringBootTest` 和不带这个注解的普通 `@Test` 有什么区别？
2. `MockMvc` 测试和用 `curl` 手动测试有什么不同？为什么 MockMvc 更好？
3. 打包出来的 jar 为什么可以直接 `java -jar` 运行？Tomcat 在哪？

---

---

---

## Week 1 总结：Spring Boot 5 天成果

```
task-manager-spring/
├── pom.xml                              # Maven 配置（5 个 starter）
├── src/main/java/com/agent/
│   ├── Application.java                 # 启动类
│   ├── controller/
│   │   └── TaskController.java          # REST API（5 个端点）
│   ├── service/
│   │   └── TaskService.java             # 业务逻辑（事务管理）
│   ├── repository/
│   │   └── TaskRepository.java          # 数据访问（JPA 接口）
│   ├── model/
│   │   └── Task.java                    # 领域实体（JPA 映射）
│   ├── dto/
│   │   ├── CreateTaskRequest.java       # 创建任务请求体
│   │   ├── UpdateTaskRequest.java       # 更新任务请求体
│   │   ├── TaskResponse.java            # 任务响应体
│   │   └── Priority.java               # 优先级枚举
│   └── exception/
│       ├── ErrorResponse.java           # 统一错误响应
│       └── GlobalExceptionHandler.java  # 全局异常处理
├── src/test/java/com/agent/
│   └── controller/
│       └── TaskControllerTest.java      # 集成测试（7 个用例）
└── src/main/resources/
    └── application.yml                  # 数据库 & JPA 配置
```

**掌握了这些技能**：

| 技能 | 具体内容 |
|------|---------|
| REST API 设计 | GET/POST/PUT/DELETE，5 个端点完整实现 |
| 参数校验 | @Valid + Bean Validation + 统一错误格式 |
| JPA 持久化 | @Entity + JpaRepository + 方法名自动生成 SQL |
| H2 数据库 | 文件模式 + H2 Console 可视化 |
| 全局异常处理 | @RestControllerAdvice 拦截所有异常 |
| DTO 模式 | 请求体/响应体/实体三层分离 |
| 集成测试 | MockMvc 模拟 HTTP 请求，7 个测试用例 |
| 打包部署 | mvn package → java -jar 一键部署 |
