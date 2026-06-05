package com.codepilot.repository.repository;

import com.codepilot.repository.document.FileContentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileContentRepository extends MongoRepository<FileContentDocument, String> {

    // Get a specific file's content by repo + path.
    Optional<FileContentDocument> findByRepositoryIdAndFilePath(String repositoryId, String filePath);

    // Get all file contents for a repository (used for bulk indexing into vector memory).
    List<FileContentDocument> findByRepositoryId(String repositoryId);

    // Delete all file contents before re-indexing on sync.
    void deleteByRepositoryId(String repositoryId);
}