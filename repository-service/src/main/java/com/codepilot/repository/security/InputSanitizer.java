package com.codepilot.repository.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes user-supplied inputs to prevent security vulnerabilities.
 *
 * Protections:
 * 1. SSRF Prevention: Validates Git URLs to block access to internal networks
 *    (localhost, 10.x, 192.168.x, 172.16-31.x, metadata endpoints).
 * 2. Path Traversal Prevention: Blocks ".." sequences and absolute paths
 *    in file path parameters.
 * 3. Repository Name Validation: Ensures names contain only safe characters.
 */
public final class InputSanitizer {

    private InputSanitizer() {} // Static utility class

    // Regex for valid repository names: alphanumeric, hyphens, underscores, dots.
    private static final Pattern VALID_REPO_NAME = Pattern.compile("^[a-zA-Z0-9._-]{1,200}$");

    // Hosts that are NEVER allowed as Git clone targets (SSRF prevention).
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "metadata.google.internal",        // GCP metadata
            "169.254.169.254"                   // AWS/Azure/GCP metadata endpoint
    );

    // Private IP range prefixes to block.
    private static final String[] PRIVATE_IP_PREFIXES = {
            "10.",          // 10.0.0.0/8
            "192.168.",     // 192.168.0.0/16
            "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31."   // 172.16.0.0/12
    };

    /**
     * Validates a Git URL to prevent SSRF attacks.
     *
     * Allowed: https://github.com/user/repo.git, git@github.com:user/repo.git
     * Blocked: http://localhost/..., http://169.254.169.254/..., http://10.0.0.1/...
     *
     * @throws IllegalArgumentException if the URL is malicious or malformed.
     */
    public static void validateGitUrl(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) return; // null is OK (local path repos)

        // Allow SSH-style URLs: git@github.com:user/repo.git
        if (gitUrl.startsWith("git@")) return;

        try {
            URL url = new URL(gitUrl);
            String host = url.getHost().toLowerCase();

            // Block known internal hostnames.
            if (BLOCKED_HOSTS.contains(host)) {
                throw new IllegalArgumentException("Git URL points to a blocked host: " + host);
            }

            // Block private IP ranges.
            for (String prefix : PRIVATE_IP_PREFIXES) {
                if (host.startsWith(prefix)) {
                    throw new IllegalArgumentException("Git URL points to a private IP range: " + host);
                }
            }

            // Block non-HTTP(S) protocols (e.g., file://, ftp://).
            String protocol = url.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException("Only HTTP/HTTPS Git URLs are allowed, got: " + protocol);
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Git URL format: " + gitUrl);
        }
    }

    /**
     * Validates a file path to prevent path traversal attacks.
     *
     * Blocked: "../../../etc/passwd", "/etc/passwd", "C:\Windows\system32"
     * Allowed: "src/main/java/App.java", "README.md"
     *
     * @throws IllegalArgumentException if the path contains traversal sequences.
     */
    public static void validateFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        // Normalize backslashes to forward slashes for consistent checking.
        String normalized = filePath.replace("\\", "/");

        // Block directory traversal.
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Path traversal detected in file path: " + filePath);
        }

        // Block absolute paths (Unix and Windows).
        if (normalized.startsWith("/") || (normalized.length() >= 2 && normalized.charAt(1) == ':')) {
            throw new IllegalArgumentException("Absolute paths are not allowed: " + filePath);
        }

        // Block null bytes (can bypass security checks in some systems).
        if (filePath.contains("\0")) {
            throw new IllegalArgumentException("Null bytes are not allowed in file paths");
        }
    }

    /**
     * Validates a repository name contains only safe characters.
     *
     * @throws IllegalArgumentException if the name is invalid.
     */
    public static void validateRepoName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repository name cannot be empty");
        }
        if (!VALID_REPO_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Repository name can only contain letters, numbers, dots, hyphens, and underscores (max 200 chars)");
        }
    }
}