package com.codepilot.repository.dto;

import com.codepilot.repository.document.FileTreeDocument.FileNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTreeResponse {
    private String repositoryId;
    private FileNode root;
    private int totalFiles;
    private int totalDirectories;
}