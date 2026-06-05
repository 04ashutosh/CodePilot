package com.codepilot.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileContentResponse {
    private String repositoryId;
    private String filePath;
    private String content;
    private int lineCount;
    private long size;
    private String language;
}