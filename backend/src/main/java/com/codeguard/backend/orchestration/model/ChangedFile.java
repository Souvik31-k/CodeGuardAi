package com.codeguard.backend.orchestration.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangedFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String filePath;
    private String patch;
}
