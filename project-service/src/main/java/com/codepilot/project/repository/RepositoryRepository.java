package com.codepilot.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepositoryRepository extends JpaRepository<com.codepilot.project.entity.Repository, UUID> {
    List<Repository> findByProjectId(UUID projectId);
}
