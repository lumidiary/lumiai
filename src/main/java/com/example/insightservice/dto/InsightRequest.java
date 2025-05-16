package com.example.insightservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class InsightRequest {
    private List<ImageData> images;
    private String user_locale;

    @Data
    public static class ImageData {
        private Metadata metadata;
    }
}
