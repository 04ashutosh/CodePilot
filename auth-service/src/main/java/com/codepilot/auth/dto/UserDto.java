package com.codepilot.auth.dto;

import com.codepilot.auth.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String email;
    private String username;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private Role role;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private Instant createdAt;
}