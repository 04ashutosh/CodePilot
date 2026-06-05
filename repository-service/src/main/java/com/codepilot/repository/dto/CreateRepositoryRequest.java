package com.codepilot.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepositoryRequest {

    @NotBlank(message = "Repository name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // The project this repo belongs to (from project-service).
    @NotBlank(message = "Project ID is required")
    private String projectId;

    // Git clone URL. Optional — if null, user must provide a local path.
    private String gitUrl;

    // Local filesystem path to an existing repo. Optional — if null, gitUrl is used.
    private String localPath;

    // Branch to track. Defaults to "main" if not provided.
    private String branch;
}