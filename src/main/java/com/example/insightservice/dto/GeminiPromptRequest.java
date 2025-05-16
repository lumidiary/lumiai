package com.example.insightservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Part {
        private String text;
        private InlineData inline_data;

        public Part(String text) {
            this.text = text;
        }
        public Part(InlineData inline_data) {
            this.inline_data = inline_data;
        }
    }

    @Data
    @AllArgsConstructor
    public static class InlineData {
        private String mime_type;
        private String data;
    }
}
