package com.codepilot.repository.service;

import com.codepilot.repository.document.FileContentDocument;
import com.codepilot.repository.document.FileTreeDocument;
import com.codepilot.repository.document.FileTreeDocument.FileNode;
import com.codepilot.repository.repository.FileContentRepository;
import com.codepilot.repository.repository.FileTreeRepository;
import com.codepilot.repository.util.LanguageDetector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recursively walks a repository directory, builds a hierarchical file tree,
 * and stores the raw content of each text file in MongoDB.
 *
 * Key design decisions:
 * - Max file size: 1MB — larger files are skipped to avoid MongoDB document bloat.
 * - Binary files: Skipped entirely (images, archives, compiled output).
 * - Ignored directories: node_modules, .git, target, build, etc.
 * - Language detection: Based on file extension mapping.
 * - Framework detection: Based on presence of key files (pom.xml, package.json, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileIndexingService {

    private final FileTreeRepository fileTreeRepository;
    private final FileContentRepository fileContentRepository;

    // Files larger than 1MB are skipped to keep MongoDB documents manageable.
    private static final long MAX_FILE_SIZE_BYTES = 1_048_576; // 1 MB

    /**
     * Indexes an entire repository: builds the file tree and stores all file contents.
     * This method clears any existing index data before rebuilding.
     *
     * @param repositoryId The MongoDB ID of the repository being indexed.
     * @param repoPath     Absolute path to the root directory of the cloned repo.
     * @return A map of metadata: totalFiles, totalDirectories, primaryLanguage, detectedFramework.
     */
    public Map<String, Object> indexRepository(String repositoryId, String repoPath) {
        File rootDir = new File(repoPath);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid repository path: " + repoPath);
        }

        log.info("Starting indexing for repository '{}' at path '{}'", repositoryId, repoPath);

        // ===== Step 1: Clear existing index data =====
        // This ensures re-syncs don't create duplicate entries.
        fileTreeRepository.deleteByRepositoryId(repositoryId);
        fileContentRepository.deleteByRepositoryId(repositoryId);

        // ===== Step 2: Walk the directory tree =====
        // We track language counts to determine the primary language.
        Map<String, Integer> languageCounts = new HashMap<>();
        List<FileContentDocument> contentBatch = new ArrayList<>();

        FileNode rootNode = walkDirectory(rootDir, rootDir, repositoryId, languageCounts, contentBatch);

        // ===== Step 3: Count totals =====
        int[] counts = countNodes(rootNode); // [files, directories]

        // ===== Step 4: Save the file tree as a single document =====
        FileTreeDocument treeDoc = FileTreeDocument.builder()
                .repositoryId(repositoryId)
                .root(rootNode)
                .totalFiles(counts[0])
                .totalDirectories(counts[1])
                .build();

        fileTreeRepository.save(treeDoc);
        log.info("File tree saved: {} files, {} directories", counts[0], counts[1]);

        // ===== Step 5: Batch-save all file contents =====
        if (!contentBatch.isEmpty()) {
            fileContentRepository.saveAll(contentBatch);
            log.info("Saved content for {} files", contentBatch.size());
        }

        // ===== Step 6: Detect primary language and framework =====
        String primaryLanguage = detectPrimaryLanguage(languageCounts);
        String framework = detectFramework(rootDir);

        // Build metadata map for updating the RepositoryDocument.
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalFiles", counts[0]);
        metadata.put("totalDirectories", counts[1]);
        metadata.put("totalIndexedContents", contentBatch.size());
        metadata.put("primaryLanguage", primaryLanguage);
        metadata.put("detectedFramework", framework);
        metadata.put("languageBreakdown", languageCounts);

        log.info("Indexing complete for '{}'. Language: {}, Framework: {}", repositoryId, primaryLanguage, framework);

        return metadata;
    }

    // =============================================
    //  PRIVATE: Recursive directory walker
    // =============================================

    /**
     * Recursively walks a directory, building FileNode tree and collecting file contents.
     *
     * @param currentDir     The directory currently being processed.
     * @param rootDir        The repo root (used to calculate relative paths).
     * @param repositoryId   The repo ID for linking file contents.
     * @param languageCounts Accumulator for language frequency tracking.
     * @param contentBatch   Accumulator for file content documents to batch-save.
     * @return A FileNode representing this directory and all its children.
     */
    private FileNode walkDirectory(File currentDir, File rootDir, String repositoryId,
                                   Map<String, Integer> languageCounts,
                                   List<FileContentDocument> contentBatch) {

        String relativePath = rootDir.toPath().relativize(currentDir.toPath()).toString()
                .replace("\\", "/"); // Normalize to forward slashes for cross-platform consistency.

        FileNode dirNode = FileNode.builder()
                .name(currentDir.getName())
                .path(relativePath.isEmpty() ? "." : relativePath)
                .type("DIRECTORY")
                .size(0)
                .children(new ArrayList<>())
                .build();

        File[] children = currentDir.listFiles();
        if (children == null) return dirNode;

        // Sort children: directories first, then files, both alphabetically.
        // This gives a clean, predictable tree structure in the UI.
        Arrays.sort(children, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File child : children) {
            if (child.isDirectory()) {
                // Skip ignored directories (node_modules, .git, target, etc.)
                if (LanguageDetector.isIgnoredDirectory(child.getName())) {
                    log.debug("Skipping ignored directory: {}", child.getName());
                    continue;
                }
                // Recurse into subdirectory.
                dirNode.getChildren().add(
                        walkDirectory(child, rootDir, repositoryId, languageCounts, contentBatch));

            } else if (child.isFile()) {
                // Process individual file.
                FileNode fileNode = processFile(child, rootDir, repositoryId, languageCounts, contentBatch);
                if (fileNode != null) {
                    dirNode.getChildren().add(fileNode);
                }
            }
        }

        return dirNode;
    }

    /**
     * Processes a single file: creates a FileNode and stores its content if eligible.
     * Returns null if the file should be skipped (binary, too large, unreadable).
     */
    private FileNode processFile(File file, File rootDir, String repositoryId,
                                 Map<String, Integer> languageCounts,
                                 List<FileContentDocument> contentBatch) {

        String fileName = file.getName();

        // Skip binary files — they can't be searched or embedded as vectors.
        if (LanguageDetector.isBinaryFile(fileName)) {
            return null;
        }

        // Skip files exceeding the size limit.
        long fileSize = file.length();
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            log.debug("Skipping oversized file ({} bytes): {}", fileSize, fileName);
            return null;
        }

        String relativePath = rootDir.toPath().relativize(file.toPath()).toString()
                .replace("\\", "/");

        String extension = LanguageDetector.getExtension(fileName);
        String language = LanguageDetector.detectLanguage(extension);

        // Track language frequency for primary language detection.
        if (language != null) {
            languageCounts.merge(language, 1, Integer::sum);
        }

        // Build the FileNode for the tree.
        FileNode fileNode = FileNode.builder()
                .name(fileName)
                .path(relativePath)
                .type("FILE")
                .size(fileSize)
                .language(language)
                .extension(extension)
                .build();

        // Read and store the file's text content.
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            int lineCount = content.split("\n", -1).length;

            FileContentDocument contentDoc = FileContentDocument.builder()
                    .repositoryId(repositoryId)
                    .filePath(relativePath)
                    .content(content)
                    .lineCount(lineCount)
                    .size(fileSize)
                    .language(language)
                    .build();

            contentBatch.add(contentDoc);

        } catch (IOException e) {
            // File might be unreadable (permissions, encoding issues) — skip silently.
            log.warn("Could not read file '{}': {}", relativePath, e.getMessage());
        } catch (Exception e) {
            // Catch encoding errors (e.g., binary files with text extensions).
            log.warn("Error processing file '{}': {}", relativePath, e.getMessage());
        }

        return fileNode;
    }

    // =============================================
    //  PRIVATE: Detection helpers
    // =============================================

    /**
     * Determines the primary language by picking the most frequent one.
     */
    private String detectPrimaryLanguage(Map<String, Integer> languageCounts) {
        return languageCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }

    /**
     * Detects the framework by checking for signature files in the repo root.
     * Checks in order of specificity — first match wins.
     */
    private String detectFramework(File rootDir) {
        // Java / JVM frameworks
        if (fileContains(rootDir, "pom.xml", "spring-boot")) return "Spring Boot";
        if (fileContains(rootDir, "build.gradle", "spring-boot")) return "Spring Boot";
        if (new File(rootDir, "pom.xml").exists()) return "Maven Project";
        if (new File(rootDir, "build.gradle").exists()) return "Gradle Project";

        // JavaScript / TypeScript frameworks
        if (fileContains(rootDir, "angular.json", "")) return "Angular";
        if (fileContains(rootDir, "next.config.js", "") ||
                fileContains(rootDir, "next.config.mjs", "") ||
                fileContains(rootDir, "next.config.ts", "")) return "Next.js";
        if (fileContains(rootDir, "nuxt.config.ts", "") ||
                fileContains(rootDir, "nuxt.config.js", "")) return "Nuxt.js";
        if (fileContains(rootDir, "vite.config.ts", "") ||
                fileContains(rootDir, "vite.config.js", "")) return "Vite";
        if (fileContains(rootDir, "package.json", "react")) return "React";
        if (fileContains(rootDir, "package.json", "vue")) return "Vue.js";
        if (fileContains(rootDir, "package.json", "express")) return "Express.js";

        // Python frameworks
        if (fileContains(rootDir, "requirements.txt", "fastapi") ||
                fileContains(rootDir, "pyproject.toml", "fastapi")) return "FastAPI";
        if (fileContains(rootDir, "requirements.txt", "django") ||
                fileContains(rootDir, "pyproject.toml", "django")) return "Django";
        if (fileContains(rootDir, "requirements.txt", "flask") ||
                fileContains(rootDir, "pyproject.toml", "flask")) return "Flask";

        // Ruby
        if (new File(rootDir, "Gemfile").exists()) return "Ruby (Bundler)";

        // Go
        if (new File(rootDir, "go.mod").exists()) return "Go Module";

        // Rust
        if (new File(rootDir, "Cargo.toml").exists()) return "Rust (Cargo)";

        // Docker
        if (new File(rootDir, "Dockerfile").exists()) return "Docker";

        return null;
    }

    /**
     * Checks if a file exists in the directory and optionally contains a keyword.
     * If keyword is empty, only checks existence.
     */
    private boolean fileContains(File dir, String fileName, String keyword) {
        File file = new File(dir, fileName);
        if (!file.exists()) return false;
        if (keyword == null || keyword.isEmpty()) return true;

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8).toLowerCase();
            return content.contains(keyword.toLowerCase());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Counts total files and directories in a FileNode tree.
     * @return int array: [fileCount, directoryCount]
     */
    private int[] countNodes(FileNode node) {
        if (node == null) return new int[]{0, 0};

        int files = 0;
        int dirs = 0;

        if ("FILE".equals(node.getType())) {
            files = 1;
        } else if ("DIRECTORY".equals(node.getType())) {
            dirs = 1; // Count this directory itself.
            if (node.getChildren() != null) {
                for (FileNode child : node.getChildren()) {
                    int[] childCounts = countNodes(child);
                    files += childCounts[0];
                    dirs += childCounts[1];
                }
            }
        }

        return new int[]{files, dirs};
    }
}