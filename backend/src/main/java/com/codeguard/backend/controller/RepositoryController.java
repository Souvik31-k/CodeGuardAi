package com.codeguard.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.codeguard.backend.dto.request.CreateRepositoryRequest;
import com.codeguard.backend.dto.request.UpdateRepositoryRequest;
import com.codeguard.backend.dto.response.CreateRepositoryResponse;
import com.codeguard.backend.dto.response.RepositoryResponse;
import com.codeguard.backend.service.RepositoryService;

import jakarta.validation.Valid;

@RestController
public class RepositoryController {
    private final RepositoryService service;

    RepositoryController(RepositoryService service) {
        this.service = service;
    }

    @PostMapping("/repositories")
    public ResponseEntity<CreateRepositoryResponse> saveRepo(@Valid @RequestBody CreateRepositoryRequest request) {

        CreateRepositoryResponse respose = service.createRepository(request);
        return new ResponseEntity<>(respose, HttpStatus.CREATED);
    }

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositoryResponse>> getRepositories() {
        List<RepositoryResponse> response = service.getAllRepositories();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/repositories/{id}")
    public ResponseEntity<?> getRepository(@PathVariable Long id) {
        return new ResponseEntity<>(service.getRepositoryById(id), HttpStatus.OK);
    }

    @PutMapping("/repositories/{id}")
    public ResponseEntity<RepositoryResponse> updateRepository(@PathVariable Long id,
            @Valid @RequestBody UpdateRepositoryRequest request) {
        return new ResponseEntity<>(service.updateRepository(id, request), HttpStatus.OK);
    }

    @PatchMapping("/repositories/{id}/deactivate")
    public ResponseEntity<String> deactivateRepoById(@PathVariable Long id) {
        service.deactivateRepository(id);
        return new ResponseEntity<>("Repository deactivated", HttpStatus.OK);
    }
}
