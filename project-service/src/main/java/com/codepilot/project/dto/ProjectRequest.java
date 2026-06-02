package com.codepilot.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    private String description;
    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;
}