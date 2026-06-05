package com.codepilot.repository.repository;

import com.codepilot.repository.document.RepositoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositoryDocumentRepository extends MongoRepository<RepositoryDocument, String> {

    // Find all repositories belonging to a specific project.
    List<RepositoryDocument> findByProjectId(String projectId);

    // Find all repositories owned by a specific user.
    List<RepositoryDocument> findByOwnerId(String ownerId);

    // Check if a repo with the same Git URL already exists in the project.
    boolean existsByProjectIdAndGitUrl(String projectId, String gitUrl);
}