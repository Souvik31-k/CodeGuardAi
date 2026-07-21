package com.codeguard.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.codeguard.backend.service.WebhookService;

@RestController
public class WebhookController {
    private final WebhookService service;

    WebhookController(WebhookService service) {
        this.service = service;
    }

    @PostMapping("/webhooks/github")
    public ResponseEntity<Void> getWebhook(@RequestHeader("X-Github-Event") String event,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Delivery") String delivery,
            @RequestBody String payload) {

        service.handleWebhook(event, signature, delivery, payload);
        return ResponseEntity.ok().build();
    }

}
