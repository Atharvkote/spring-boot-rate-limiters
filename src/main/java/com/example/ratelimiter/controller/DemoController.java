package com.example.ratelimiter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/** Minimal demo endpoints for testing each rate limiter category. */
@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() { return ok("General - /api/test"); }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> products() { return ok("General - /api/products"); }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login() { return ok("Auth - /api/auth/login"); }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload() { return ok("Upload - /api/upload"); }

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> payment() { return ok("Sensitive - /api/payment"); }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        return ok("Sensitive - DELETE /api/users/" + id);
    }

    private ResponseEntity<Map<String, Object>> ok(String msg) {
        return ResponseEntity.ok(Map.of("message", msg, "timestamp", Instant.now().toString()));
    }
}
