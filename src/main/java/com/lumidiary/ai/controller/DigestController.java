package com.lumidiary.ai.controller;

import com.lumidiary.ai.dto.DigestRequest;
import com.lumidiary.ai.dto.DigestResponse;
import com.lumidiary.ai.service.DigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/digest")
public class DigestController {

    private final DigestService digestService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody DigestRequest request) {
        try {
            DigestResponse response = digestService.createDigest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Digest creation failed: " + e.getMessage());
        }
    }
}
