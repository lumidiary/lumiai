package com.lumidiary.ai.controller;

import com.lumidiary.ai.dto.GeminiResponse;
import com.lumidiary.ai.dto.InsightRequest;
import com.lumidiary.ai.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insight")
public class InsightController {

    private final InsightService insightService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody InsightRequest request) {
        try {
            GeminiResponse response = insightService.analyze(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Insight 분석 실패: " + e.getMessage());
        }
    }
}
