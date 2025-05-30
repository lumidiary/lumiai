package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class VisionRequest {
    private List<ImageData> images;
    @JsonProperty("user_locale")
    private String user_locale;

    @Data
    public static class ImageData {
        private String id;
        private String url;
    }
}
