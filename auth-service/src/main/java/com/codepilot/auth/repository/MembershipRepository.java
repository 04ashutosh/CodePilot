package com.codepilot.auth.repository;

import com.codepilot.auth.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByUserId(UUID userId);
    List<Membership> findByWorkspaceId(UUID workspaceId);
    Optional<Membership> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
    boolean existsByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
}