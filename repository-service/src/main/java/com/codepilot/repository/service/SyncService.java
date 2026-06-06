package com.codepilot.repository.service;

import com.codepilot.repository.document.RepositoryDocument;
import com.codepilot.repository.repository.RepositoryDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Orchestrates repository sync: clone/pull + file indexing.
 *
 * The sync is triggered via the REST API (POST /api/repos/{id}/sync)
 * and runs asynchronously in a background thread. The caller receives
 * an immediate response with syncStatus = "SYNCING".
 *
 * Workflow:
 * 1. Set status to SYNCING.
 * 2. Clone (first sync) or Pull (re-sync) the repository.
 * 3. Walk the file tree and index all contents.
 * 4. Update metadata (language, framework, file counts).
 * 5. Set status to SYNCED (or FAILED on error).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final RepositoryDocumentRepository repoRepository;
    private final GitService gitService;
    private final FileIndexingService fileIndexingService;

    /**
     * Triggers an async repository sync.
     * The @Async annotation makes this run on the "repoSyncExecutor" thread pool
     * defined in AsyncConfig, so the calling controller thread returns immediately.
     */
    @Async("repoSyncExecutor")
    public void syncRepository(String repositoryId) {
        log.info("Starting async sync for repository '{}'", repositoryId);

        RepositoryDocument repo = repoRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        // ===== Step 1: Mark as SYNCING =====
        repo.setSyncStatus("SYNCING");
        repoRepository.save(repo);

        try {
            // ===== Step 2: Clone or Pull =====
            String localPath;

            if (repo.getLocalPath() != null && !repo.getLocalPath().isEmpty()
                    && "SYNCED".equals(repo.getSyncStatus()) == false
                    && repo.getGitUrl() != null) {
                // Re-sync: pull latest changes into existing clone.
                // But only if we have a gitUrl (local-only repos don't need pull).
                if (isAlreadyCloned(repo)) {
                    log.info("Repository already cloned, pulling latest changes...");
                    gitService.pullLatest(repo.getLocalPath());
                    localPath = repo.getLocalPath();
                } else {
                    // First clone from remote.
                    localPath = gitService.cloneRepository(repositoryId, repo.getGitUrl(), repo.getBranch());
                }
            } else if (repo.getGitUrl() != null) {
                // First sync with remote URL — clone it.
                localPath = gitService.cloneRepository(repositoryId, repo.getGitUrl(), repo.getBranch());
            } else {
                // Local path repo — just validate and use the provided path.
                localPath = gitService.validateLocalPath(repo.getLocalPath());
            }

            // Update the local path in case it was just cloned.
            repo.setLocalPath(localPath);

            // ===== Step 3: Index the repository files =====
            Map<String, Object> metadata = fileIndexingService.indexRepository(repositoryId, localPath);

            // ===== Step 4: Update repository with results =====
            repo.setLanguage((String) metadata.get("primaryLanguage"));
            repo.setFramework((String) metadata.get("detectedFramework"));
            repo.setMetadata(metadata);
            repo.setSyncStatus("SYNCED");
            repo.setLastSyncedAt(Instant.now());

            repoRepository.save(repo);
            log.info("Sync completed successfully for repository '{}'", repositoryId);

        } catch (Exception e) {
            // ===== Handle failure: mark as FAILED =====
            log.error("Sync failed for repository '{}': {}", repositoryId, e.getMessage(), e);
            repo.setSyncStatus("FAILED");
            repo.setMetadata(Map.of("error", e.getMessage()));
            repoRepository.save(repo);
        }
    }

    /**
     * Checks if the repo directory already contains a .git folder,
     * indicating it was previously cloned.
     */
    private boolean isAlreadyCloned(RepositoryDocument repo) {
        if (repo.getLocalPath() == null) return false;
        java.io.File gitDir = new java.io.File(repo.getLocalPath(), ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
}