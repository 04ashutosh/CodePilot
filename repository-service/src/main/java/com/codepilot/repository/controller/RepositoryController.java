package com.codepilot.repository.controller;

import com.codepilot.repository.dto.*;
import com.codepilot.repository.service.RepositoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    /**
     * POST /api/repos — Register a new repository.
     * The owner ID is extracted from the JWT by the gateway and forwarded
     * as the X-User-Username header. In Phase 2.3 (Security), we'll switch
     * this to extract directly from the JWT in a local filter.
     */
    @PostMapping
    public ResponseEntity<RepositoryResponse> createRepository(
            @Valid @RequestBody CreateRepositoryRequest request,
            @RequestHeader(value = "X-User-Username", required = false) String username) {

        // Use the username as ownerId for now.
        // Phase 2.3 will replace this with proper JWT-extracted user ID.
        String ownerId = username != null ? username : "anonymous";

        RepositoryResponse response = repositoryService.createRepository(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/repos/{id} — Get a repository by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RepositoryResponse> getRepository(@PathVariable String id) {
        return ResponseEntity.ok(repositoryService.getRepository(id));
    }

    /**
     * GET /api/repos?projectId={projectId} — List all repositories for a project.
     */
    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> getRepositories(
            @RequestParam String projectId) {
        return ResponseEntity.ok(repositoryService.getRepositoriesByProject(projectId));
    }

    /**
     * GET /api/repos/{id}/tree — Get the file tree of a repository.
     * Requires the repo to be synced first (Phase 2.2).
     */
    @GetMapping("/{id}/tree")
    public ResponseEntity<FileTreeResponse> getFileTree(@PathVariable String id) {
        return ResponseEntity.ok(repositoryService.getFileTree(id));
    }

    /**
     * GET /api/repos/{id}/files?path={filePath} — Get raw content of a file.
     * The path parameter is relative to the repo root.
     * Example: /api/repos/abc123/files?path=src/main/java/App.java
     */
    @GetMapping("/{id}/files")
    public ResponseEntity<FileContentResponse> getFileContent(
            @PathVariable String id,
            @RequestParam String path) {
        return ResponseEntity.ok(repositoryService.getFileContent(id, path));
    }

    /**
     * DELETE /api/repos/{id} — Delete a repository and all associated data.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable String id) {
        repositoryService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }

//    This clones the repo (first time) or pulls latest changes (re-sync),
//    then walks the file tree and indexes all file contents into MongoDB.
    @PostMapping("/{id}/sync")
    public ResponseEntity<RepositoryResponse> syncRepository(@PathVariable String id) {
        RepositoryResponse response = repositoryService.triggerSync(id);
        return ResponseEntity.accepted().body(response);
    }
}