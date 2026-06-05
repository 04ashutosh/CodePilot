package com.codepilot.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {
    private String id;
    private String projectId;
    private String ownerId;
    private String name;
    private String description;
    private String gitUrl;
    private String localPath;
    private String branch;
    private String language;
    private String framework;
    private String syncStatus;
    private Map<String, Object> metadata;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;
}