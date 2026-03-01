package com.bitespeed.identity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class IdentityController {

    private final IdentityService service;

    public IdentityController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/identify")
    public ResponseEntity<IdentifyResponse> identify(@RequestBody IdentifyRequest request) {
        try {
            return ResponseEntity.ok(service.identify(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/")
    public String health() {
    return "Bitespeed Identity Service is running";
}
}