package com.codepilot.repository.repository;

import com.codepilot.repository.document.FileTreeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileTreeRepository extends MongoRepository<FileTreeDocument, String> {

    // Fetch the file tree for a given repository (one tree per repo).
    Optional<FileTreeDocument> findByRepositoryId(String repositoryId);

    // Delete existing tree before re-indexing on sync.
    void deleteByRepositoryId(String repositoryId);
}