package com.example.insightservice.controller;

import com.example.insightservice.dto.GeminiResponse;
import com.example.insightservice.dto.InsightRequest;
import com.example.insightservice.integration.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insight")
public class InsightController {

    private final GeminiApiClient geminiApiClient;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody InsightRequest request) {
        try {
            GeminiResponse response = geminiApiClient.requestToGemini(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Gemini 호출 실패: " + e.getMessage());
        }
    }
}
