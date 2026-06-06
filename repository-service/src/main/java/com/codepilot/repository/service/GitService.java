package com.codepilot.repository.service;

import com.codepilot.repository.exception.RepositoryOperationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages Git operations: clone, pull, and local path validation.
 *
 * Each repository is cloned into: {base-path}/{repositoryId}/
 * This isolation ensures repos never interfere with each other.
 */
@Slf4j
@Service
public class GitService {

    @Value("${app.repos.base-path:./codepilot-repos}")
    private String reposBasePath;

    /**
     * Clones a remote Git repository into a local directory.
     *
     * @param repositoryId Unique ID used as the directory name.
     * @param gitUrl       Remote URL to clone from (HTTPS or SSH).
     * @param branch       Branch to checkout after cloning.
     * @return Absolute path to the cloned repository directory.
     */
    public String cloneRepository(String repositoryId, String gitUrl, String branch) {
        Path targetDir = Paths.get(reposBasePath, repositoryId);

        try {
            // Create the parent directories if they don't exist.
            Files.createDirectories(targetDir);

            log.info("Cloning repository '{}' from '{}' (branch: {}) into '{}'",
                    repositoryId, gitUrl, branch, targetDir);

            // JGit clone: fetches all refs and checks out the specified branch.
            // Auto-closeable Git object wraps the underlying repository.
            Git git = Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(targetDir.toFile())
                    .setBranch(branch)
                    .setCloneAllBranches(false)     // Only clone the target branch (faster)
                    .setDepth(1)                     // Shallow clone — we only need latest code, not history
                    .call();

            git.close();

            String absolutePath = targetDir.toAbsolutePath().toString();
            log.info("Clone completed successfully: {}", absolutePath);
            return absolutePath;

        } catch (GitAPIException e) {
            throw new RepositoryOperationException(
                    "Failed to clone repository from " + gitUrl + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RepositoryOperationException(
                    "Failed to create directory for repository: " + e.getMessage(), e);
        }
    }

    /**
     * Pulls latest changes from the remote for an already-cloned repository.
     * Used during re-sync to update the code before re-indexing.
     *
     * @param localPath Absolute path to the existing cloned repository.
     */
    public void pullLatest(String localPath) {
        try {
            Git git = Git.open(new File(localPath));

            log.info("Pulling latest changes for repository at '{}'", localPath);

            PullResult result = git.pull().call();

            if (result.isSuccessful()) {
                log.info("Pull successful for '{}'", localPath);
            } else {
                log.warn("Pull completed with issues for '{}': {}", localPath, result);
            }

            git.close();

        } catch (GitAPIException e) {
            throw new RepositoryOperationException(
                    "Failed to pull latest changes: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RepositoryOperationException(
                    "Failed to open repository at " + localPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a local filesystem path is an existing directory.
     * Used when the user registers a local repo (no Git URL provided).
     *
     * @param localPath Path provided by the user.
     * @return Absolute, normalized path string.
     */
    public String validateLocalPath(String localPath) {
        Path path = Paths.get(localPath);

        if (!Files.exists(path)) {
            throw new RepositoryOperationException("Local path does not exist: " + localPath);
        }
        if (!Files.isDirectory(path)) {
            throw new RepositoryOperationException("Local path is not a directory: " + localPath);
        }

        return path.toAbsolutePath().normalize().toString();
    }
}