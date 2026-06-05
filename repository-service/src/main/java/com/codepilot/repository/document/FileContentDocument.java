package com.codepilot.repository.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.Instant;

/**
 * Stores the raw text content of a single file from a repository.
 * Compound index on (repositoryId + filePath) ensures uniqueness
 * and enables fast lookups by repo + path.
 *
 * Binary files (images, compiled artifacts) are NOT stored here —
 * they are skipped during the indexing phase.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_contents")
@CompoundIndex(name = "repo_path_idx", def = "{'repositoryId': 1, 'filePath': 1}", unique = true)
public class FileContentDocument {

    @Id
    private String id;

    private String repositoryId;

    // Relative path from repo root: "src/main/java/com/codepilot/auth/AuthApplication.java"
    private String filePath;

    // Raw text content of the file.
    private String content;

    // Total number of lines (useful for chunking in vector-memory-service).
    private int lineCount;

    // File size in bytes.
    private long size;

    // Detected programming language.
    private String language;

    @CreatedDate
    private Instant createdAt;
}