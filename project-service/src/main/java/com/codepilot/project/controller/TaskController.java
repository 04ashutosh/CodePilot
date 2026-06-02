package com.codepilot.project.controller;

import com.codepilot.project.dto.TaskResponse;
import com.codepilot.project.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable("id") UUID id) {
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TaskResponse> cancelTask(@PathVariable("id") UUID id) {
        TaskResponse task = taskService.cancelTask(id);
        return ResponseEntity.ok(task);
    }
}