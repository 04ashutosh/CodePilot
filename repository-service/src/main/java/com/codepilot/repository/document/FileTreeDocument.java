package com.codepilot.repository.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;

/**
 * Stores the complete file tree of a repository as a single MongoDB document.
 * The tree is represented as a nested structure of FileNode objects.
 *
 * Design decision: We store the entire tree in one document (instead of one
 * document per file) because typical repos have 100-10,000 files, which fits
 * well within MongoDB's 16MB document limit. This allows fetching the full
 * tree in a single query — much faster than assembling it from thousands of rows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_trees")
public class FileTreeDocument {

    @Id
    private String id;

    // Links to the parent RepositoryDocument.
    // Unique index ensures one tree per repo.
    @Indexed(unique = true)
    private String repositoryId;

    // Root node of the file tree (contains nested children).
    private FileNode root;

    // Total counts for quick stats without traversing the tree.
    private int totalFiles;
    private int totalDirectories;

    @CreatedDate
    private Instant createdAt;

    /**
     * Represents a single node (file or directory) in the tree.
     * Directories have children; files have size and language metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileNode {
        private String name;           // e.g., "AuthService.java"
        private String path;           // Relative path: "src/main/java/com/codepilot/auth/service/AuthService.java"
        private String type;           // "FILE" or "DIRECTORY"
        private long size;             // File size in bytes (0 for directories)
        private String language;       // Detected language (null for directories)
        private String extension;      // File extension without dot: "java", "ts", "py"
        private List<FileNode> children; // Non-null only for directories
    }
}