package com.codeguard.backend.orchestration.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class FileClassification {
    private String filePath;
    private FileCategory category;
}
