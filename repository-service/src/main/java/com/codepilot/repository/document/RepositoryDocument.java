package com.codepilot.repository.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a registered Git repository in the system.
 * Stored in the 'repositories' MongoDB collection.
 *
 * Each repository belongs to a project (from project-service) and
 * tracks Git metadata like branch, clone URL, and detected language/framework.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repositories")
public class RepositoryDocument {

    @Id
    private String id;

    // Links back to the project in project-service's PostgreSQL database.
    // Indexed for fast lookups when listing repos by project.
    @Indexed
    private String projectId;

    // The user who registered this repository.
    @Indexed
    private String ownerId;

    private String name;
    private String description;

    // Git remote URL (e.g., "https://github.com/user/repo.git")
    // Can be null if the repo is local-only.
    private String gitUrl;

    // Absolute path on the server where the repo is cloned.
    // Set automatically after a successful clone/sync.
    private String localPath;

    // The Git branch to track (default: "main").
    private String branch;

    // Auto-detected primary programming language (e.g., "Java", "Python").
    private String language;

    // Auto-detected framework (e.g., "Spring Boot", "FastAPI", "Angular").
    private String framework;

    // Current sync status: PENDING, SYNCING, SYNCED, FAILED
    private String syncStatus;

    // Flexible key-value metadata (e.g., total file count, repo size).
    private Map<String, Object> metadata;

    private Instant lastSyncedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}