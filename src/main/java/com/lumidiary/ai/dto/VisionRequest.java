package com.lumidiary.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VisionRequest {

    private String diaryId; // 있어도 되고 없어도 됨

    @JsonProperty("userLocale")  // JSON에서는 userLocale → Java에서는 userLocale
    private String userLocale;

    @JsonProperty("imgPars")  // JSON에서는 imgPars → Java에서는 images
    private List<ImageData> images;

    @Data
    public static class ImageData {
        private String id;

        @JsonProperty("accessUri")  // JSON에서는 accessUri → Java에서는 url
        private String url;
    }
}

