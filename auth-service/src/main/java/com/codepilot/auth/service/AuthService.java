package com.codepilot.auth.service;

import com.codepilot.auth.dto.*;
import com.codepilot.auth.entity.*;
import com.codepilot.auth.enums.Role;
import com.codepilot.auth.exception.AuthException;
import com.codepilot.auth.repository.*;
import com.codepilot.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username is already taken", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email is already registered", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .isVerified(false)
                .build();

        user = userRepository.save(user);

        // Generate URL-friendly default workspace slug
        String workspaceSlug = user.getUsername().toLowerCase().replaceAll("[^a-z0-9]", "") + "-workspace";
        if (workspaceRepository.existsBySlug(workspaceSlug)) {
            workspaceSlug += "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        Workspace workspace = Workspace.builder()
                .name(user.getUsername() + "'s Workspace")
                .slug(workspaceSlug)
                .description("Default personal workspace for " + user.getUsername())
                .owner(user)
                .build();

        workspace = workspaceRepository.save(workspace);

        Membership membership = Membership.builder()
                .user(user)
                .workspace(workspace)
                .role(Role.ADMIN)
                .build();

        membershipRepository.save(membership);

        log.info("User {} successfully registered with default workspace {}", user.getUsername(), workspace.getName());

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(Role.ADMIN)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new AuthException("Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!user.getIsActive()) {
            throw new AuthException("Account is inactive", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();

        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        Role role = determineUserRole(user);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(userDto)
                .build();
    }

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (!refreshToken.isUsable()) {
            throw new AuthException("Refresh token is expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = refreshToken.getUser();
        if (!user.getIsActive()) {
            throw new AuthException("User is inactive", HttpStatus.FORBIDDEN);
        }

        // Generate new Access Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());

        // Revoke the old refresh token (Refresh Token Rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate a new refresh token
        String newRefreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();
        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenValue)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        Role role = determineUserRole(user);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(userDto)
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("Token not found", HttpStatus.BAD_REQUEST));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        Role role = determineUserRole(user);

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserDto updateProfile(String username, UserDto updateDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        if (updateDto.getFullName() != null) {
            user.setFullName(updateDto.getFullName());
        }
        if (updateDto.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDto.getAvatarUrl());
        }

        user = userRepository.save(user);

        Role role = determineUserRole(user);

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> getUserWorkspaces(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        return workspaceRepository.findByOwnerId(user.getId()).stream()
                .map(w -> WorkspaceDto.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .slug(w.getSlug())
                        .description(w.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private Role determineUserRole(User user) {
        if (user.getMemberships() == null || user.getMemberships().isEmpty()) {
            return Role.DEVELOPER;
        }
        return user.getMemberships().stream()
                .filter(m -> m.getWorkspace().getOwner().getId().equals(user.getId()))
                .map(Membership::getRole)
                .findFirst()
                .orElse(user.getMemberships().stream()
                        .map(Membership::getRole)
                        .findFirst()
                        .orElse(Role.DEVELOPER));
    }
}