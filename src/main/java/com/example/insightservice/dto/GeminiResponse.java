package com.example.insightservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class GeminiResponse {
    private List<ImageDescription> imageDescriptions;
    private String overallDaySummary;
    private List<String> questions;
    private String language;

    @Data
    public static class ImageDescription {
        private int index;
        private String description;
    }
}
