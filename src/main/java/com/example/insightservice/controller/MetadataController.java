package com.example.insightservice.controller;

import com.example.insightservice.dto.Metadata;
import com.example.insightservice.util.MetadataExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataExtractor extractor;

    @PostMapping(value = "/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> extractMetadata(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "includeAddress", defaultValue = "false") boolean includeAddress,
            @RequestParam(value = "includeLandmarks", defaultValue = "false") boolean includeLandmarks,
            @RequestParam(value = "landmarkRadius", defaultValue = "1000") int landmarkRadius) {

        try {
            Metadata result = extractor.extractMetadata(imageFile, includeAddress, includeLandmarks, landmarkRadius);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ObjectNode error = extractor.getObjectMapper().createObjectNode();
            error.put("error", "메타데이터 추출 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping(value = "/extract/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> extractAllMetadata(@RequestParam("image") MultipartFile imageFile) {
        try {
            JsonNode resultNode = extractor.extractAllMetadataTags(imageFile);
            return ResponseEntity.ok(resultNode);
        } catch (Exception e) {
            ObjectNode error = extractor.getObjectMapper().createObjectNode();
            error.put("error", "전체 메타데이터 추출 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
