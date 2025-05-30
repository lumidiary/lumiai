package com.lumidiary.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumidiary.ai.dto.DigestRequest;
import com.lumidiary.ai.dto.VisionRequest;
import com.lumidiary.ai.queue.QueueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueTestController {

    private final QueueProducer queueProducer;
    private final ObjectMapper objectMapper;

    // 공통 JSON 직접 입력용
    @PostMapping("/push")
    public ResponseEntity<String> pushRaw(@RequestBody String json) {
        queueProducer.sendRaw(json); // send → sendRaw 로 수정 (직렬화 방지)
        return ResponseEntity.ok("Raw message sent to OCI Queue.");
    }

    // DigestRequest DTO 기반 메시지 입력
    @PostMapping("/digest")
    public ResponseEntity<String> pushDigest(@RequestBody DigestRequest dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            queueProducer.sendRaw(json); // 이걸로만 바꾸면 완벽하게 해결됩니다
            ;
            return ResponseEntity.ok("DigestRequest message sent.");
        } catch (Exception e) {
            log.error("Failed to serialize DigestRequest", e);
            return ResponseEntity.badRequest().body("Invalid DigestRequest");
        }
    }

    // VisionRequest DTO 기반 메시지 입력
    @PostMapping("/vision")
    public ResponseEntity<String> pushVision(@RequestBody VisionRequest dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            queueProducer.sendRaw(json);
            return ResponseEntity.ok("VisionRequest message sent.");
        } catch (Exception e) {
            log.error("Failed to serialize VisionRequest", e);
            return ResponseEntity.badRequest().body("Invalid VisionRequest");
        }
    }
}
