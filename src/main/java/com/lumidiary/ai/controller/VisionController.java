package com.lumidiary.ai.controller;

import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.dto.VisionRequest;
import com.lumidiary.ai.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vision")
public class VisionController {

    private final VisionService visionService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody VisionRequest request) {
        try {
            GeminiResponse response = visionService.analyze(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Vision 분석 실패: " + e.getMessage());
        }
    }
}
