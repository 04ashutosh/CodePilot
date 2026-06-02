package com.codepilot.project.controller;

import com.codepilot.project.dto.ProjectRequest;
import com.codepilot.project.dto.ProjectResponse;
import com.codepilot.project.dto.TaskRequest;
import com.codepilot.project.dto.TaskResponse;
import com.codepilot.project.service.ProjectService;
import com.codepilot.project.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @RequestParam("workspaceId") UUID workspaceId) {
        List<ProjectResponse> projects = projectService.getProjectsByWorkspace(workspaceId);
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = projectService.createProject(request, userId);
        return new ResponseEntity<>(project, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable("id") UUID id) {
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = projectService.updateProject(id, request);
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // Task sub-routes for projects
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getProjectTasks(@PathVariable("id") UUID id) {
        List<TaskResponse> tasks = taskService.getTasksByProject(id);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> createProjectTask(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.createTask(id, request, userId);
        return new ResponseEntity<>(task, HttpStatus.CREATED);
    }
}