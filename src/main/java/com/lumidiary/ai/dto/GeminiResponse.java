package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.List;

@Data
public class GeminiResponse {
    private List<ImageDescription> images;
    private String overallDaySummary;
    private List<String> questions;
    private String language;

    @Data
    public static class ImageDescription {
        @JsonIgnore
        private int index;
        private String imageId;
        private String description;
        private Metadata metadata;
    }
}
