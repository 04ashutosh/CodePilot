package com.codepilot.project.dto;

import com.codepilot.project.enums.TaskPriority;
import com.codepilot.project.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID id;
    private UUID projectId;
    private UUID repositoryId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private UUID createdBy;
    private UUID assignedTo;
    private String result;
    private String diffContent;
    private String explanation;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}