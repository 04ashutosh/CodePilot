package com.codepilot.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID workspaceId;
    private UUID ownerId;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}