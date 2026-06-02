package com.codepilot.project.service;

import com.codepilot.project.dto.TaskRequest;
import com.codepilot.project.dto.TaskResponse;
import com.codepilot.project.entity.Project;
import com.codepilot.project.entity.Repository;
import com.codepilot.project.entity.Task;
import com.codepilot.project.enums.TaskPriority;
import com.codepilot.project.enums.TaskStatus;
import com.codepilot.project.exception.ResourceNotFoundException;
import com.codepilot.project.repository.ProjectRepository;
import com.codepilot.project.repository.RepositoryRepository;
import com.codepilot.project.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final RepositoryRepository repositoryRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(UUID projectId) {
        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(UUID projectId, TaskRequest request, UUID creatorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Repository repository = null;
        if (request.getRepositoryId() != null) {
            repository = repositoryRepository.findById(request.getRepositoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        }

        Task task = Task.builder()
                .project(project)
                .repository(repository)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .createdBy(creatorId)
                .build();

        task = taskRepository.save(task);

        // Deferring Kafka event publication to Phase 2. Just log it for now.
        log.info("Task created: ID={}, title='{}', project={}. Event logged (Kafka deferred to Phase 2).",
                task.getId(), task.getTitle(), project.getName());

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a task that has already finished");
        }

        task.setStatus(TaskStatus.CANCELLED);
        task = taskRepository.save(task);

        log.info("Task cancelled: ID={}", task.getId());
        return mapToResponse(task);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .repositoryId(task.getRepository() != null ? task.getRepository().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .createdBy(task.getCreatedBy())
                .assignedTo(task.getAssignedTo())
                .result(task.getResult())
                .diffContent(task.getDiffContent())
                .explanation(task.getExplanation())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}