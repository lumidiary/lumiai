package com.lumidiary.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class VisionRequest {
    private List<ImageData> images;
    private String user_locale;

    @Data
    public static class ImageData {
        private String id;
        private String url;
    }
}
