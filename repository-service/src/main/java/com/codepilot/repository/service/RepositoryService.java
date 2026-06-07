package com.codepilot.repository.service;

import com.codepilot.repository.document.RepositoryDocument;
import com.codepilot.repository.document.FileTreeDocument;
import com.codepilot.repository.document.FileContentDocument;
import com.codepilot.repository.dto.*;
import com.codepilot.repository.exception.ResourceNotFoundException;
import com.codepilot.repository.exception.RepositoryOperationException;
import com.codepilot.repository.repository.RepositoryDocumentRepository;
import com.codepilot.repository.repository.FileTreeRepository;
import com.codepilot.repository.repository.FileContentRepository;
import com.codepilot.repository.security.InputSanitizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryDocumentRepository repoRepository;
    private final FileTreeRepository fileTreeRepository;
    private final FileContentRepository fileContentRepository;
    private final SyncService syncService;

    // Injected from application.yml: app.repos.base-path
    @Value("${app.repos.base-path:./codepilot-repos}")
    private String reposBasePath;

    /**
     * Registers a new repository in the system.
     * At this point, no cloning happens — the repo is just recorded with PENDING status.
     * Cloning/indexing is triggered separately via the /sync endpoint.
     */
    public RepositoryResponse createRepository(CreateRepositoryRequest request, String ownerId) {
        // Validate that at least one source is provided.
        if (request.getGitUrl() == null && request.getLocalPath() == null) {
            throw new IllegalArgumentException("Either gitUrl or localPath must be provided");
        }

        InputSanitizer.validateGitUrl(request.getGitUrl());
        InputSanitizer.validateRepoName(request.getName());

        // Prevent duplicate registrations of the same Git URL in a project.
        if (request.getGitUrl() != null &&
                repoRepository.existsByProjectIdAndGitUrl(request.getProjectId(), request.getGitUrl())) {
            throw new IllegalArgumentException("A repository with this Git URL already exists in the project");
        }

        RepositoryDocument doc = RepositoryDocument.builder()
                .projectId(request.getProjectId())
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .gitUrl(request.getGitUrl())
                .localPath(request.getLocalPath())
                .branch(request.getBranch() != null ? request.getBranch() : "main")
                .syncStatus("PENDING")
                .build();

        RepositoryDocument saved = repoRepository.save(doc);
        log.info("Repository '{}' registered with ID: {}", saved.getName(), saved.getId());

        return toResponse(saved);
    }

    /**
     * Fetches a single repository by its ID.
     */
    public RepositoryResponse getRepository(String id) {
        return toResponse(findRepoOrThrow(id));
    }

    /**
     * Lists all repositories belonging to a project.
     */
    public List<RepositoryResponse> getRepositoriesByProject(String projectId) {
        return repoRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetches the file tree for a repository.
     * Returns null-safe response if the tree hasn't been built yet (repo not synced).
     */
    public FileTreeResponse getFileTree(String repositoryId) {
        // Verify the repository exists first.
        findRepoOrThrow(repositoryId);

        FileTreeDocument tree = fileTreeRepository.findByRepositoryId(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File tree not found. Please sync the repository first."));

        return FileTreeResponse.builder()
                .repositoryId(tree.getRepositoryId())
                .root(tree.getRoot())
                .totalFiles(tree.getTotalFiles())
                .totalDirectories(tree.getTotalDirectories())
                .build();
    }

    /**
     * Fetches the raw content of a specific file from a repository.
     * The filePath is relative to the repo root (e.g., "src/main/java/App.java").
     */
    public FileContentResponse getFileContent(String repositoryId, String filePath) {
        // Verify the repository exists.
        findRepoOrThrow(repositoryId);

        InputSanitizer.validateFilePath(filePath);

        FileContentDocument content = fileContentRepository
                .findByRepositoryIdAndFilePath(repositoryId, filePath)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found: " + filePath));

        return FileContentResponse.builder()
                .repositoryId(content.getRepositoryId())
                .filePath(content.getFilePath())
                .content(content.getContent())
                .lineCount(content.getLineCount())
                .size(content.getSize())
                .language(content.getLanguage())
                .build();
    }

    /**
     * Deletes a repository and all associated data (file tree + contents).
     */
    public void deleteRepository(String id) {
        RepositoryDocument repo = findRepoOrThrow(id);

        // Cascade delete: remove file tree and all file contents first.
        fileTreeRepository.deleteByRepositoryId(id);
        fileContentRepository.deleteByRepositoryId(id);
        repoRepository.delete(repo);

        log.info("Repository '{}' (ID: {}) deleted with all associated data", repo.getName(), id);
    }

    /**
     * Triggers an asynchronous sync (clone/pull + index) for a repository.
     * Returns immediately — the sync runs in a background thread.
     */
    public RepositoryResponse triggerSync(String id) {
        RepositoryDocument repo = findRepoOrThrow(id);

        // Prevent syncing a repo that is already being synced.
        if ("SYNCING".equals(repo.getSyncStatus())) {
            throw new IllegalArgumentException("Repository is already syncing");
        }

        // Fire and forget — sync runs asynchronously.
        syncService.syncRepository(id);

        // Return the current state (status will be SYNCING).
        repo.setSyncStatus("SYNCING");
        return toResponse(repo);
    }

    // ========================
    // Private helper methods
    // ========================

    private RepositoryDocument findRepoOrThrow(String id) {
        return repoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + id));
    }

    private RepositoryResponse toResponse(RepositoryDocument doc) {
        return RepositoryResponse.builder()
                .id(doc.getId())
                .projectId(doc.getProjectId())
                .ownerId(doc.getOwnerId())
                .name(doc.getName())
                .description(doc.getDescription())
                .gitUrl(doc.getGitUrl())
                .localPath(doc.getLocalPath())
                .branch(doc.getBranch())
                .language(doc.getLanguage())
                .framework(doc.getFramework())
                .syncStatus(doc.getSyncStatus())
                .metadata(doc.getMetadata())
                .lastSyncedAt(doc.getLastSyncedAt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}