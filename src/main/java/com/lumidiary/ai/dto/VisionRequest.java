package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class VisionRequest {
    private String id;
    private List<ImageData> images;
    private String userLocale;

    @Data
    public static class ImageData {
        private String id;
        private String url;
    }
}
