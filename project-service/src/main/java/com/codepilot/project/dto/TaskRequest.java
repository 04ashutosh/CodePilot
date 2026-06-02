package com.codepilot.project.dto;

import com.codepilot.project.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    private UUID repositoryId;
    private TaskPriority priority;
}