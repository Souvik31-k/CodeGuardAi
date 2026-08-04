package com.codeguard.backend.orchestration.dto;

import java.util.List;

import com.codeguard.backend.orchestration.model.FileClassification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorClassificationResponse {
    private List<FileClassification> classifications;
}
