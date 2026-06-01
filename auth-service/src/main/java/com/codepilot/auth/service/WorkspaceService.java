package com.codepilot.auth.service;

import com.codepilot.auth.entity.Membership;
import com.codepilot.auth.entity.User;
import com.codepilot.auth.entity.Workspace;
import com.codepilot.auth.enums.Role;
import com.codepilot.auth.exception.AuthException;
import com.codepilot.auth.repository.MembershipRepository;
import com.codepilot.auth.repository.UserRepository;
import com.codepilot.auth.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public Workspace createWorkspace(String name, String slug, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        if (workspaceRepository.existsBySlug(slug)) {
            throw new AuthException("Workspace slug already exists", HttpStatus.BAD_REQUEST);
        }

        Workspace workspace = Workspace.builder()
                .name(name)
                .slug(slug)
                .owner(owner)
                .build();

        workspace = workspaceRepository.save(workspace);

        Membership membership = Membership.builder()
                .user(owner)
                .workspace(workspace)
                .role(Role.ADMIN)
                .build();

        membershipRepository.save(membership);

        return workspace;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getUserWorkspaces(UUID userId) {
        return workspaceRepository.findByOwnerId(userId);
    }

    @Transactional
    public void addMember(UUID workspaceId, String usernameOrEmail, Role role) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new AuthException("Workspace not found", HttpStatus.NOT_FOUND));

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        if (membershipRepository.existsByUserIdAndWorkspaceId(user.getId(), workspaceId)) {
            throw new AuthException("User is already a member of this workspace", HttpStatus.BAD_REQUEST);
        }

        Membership membership = Membership.builder()
                .user(user)
                .workspace(workspace)
                .role(role)
                .build();

        membershipRepository.save(membership);
    }
}