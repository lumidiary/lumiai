package com.example.insightservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
public class GeminiPromptRequest {
    private List<Content> contents;

    @Data
    @AllArgsConstructor
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Data
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}
