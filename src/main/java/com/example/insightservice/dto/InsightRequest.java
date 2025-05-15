package com.example.insightservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class    InsightRequest {
    private List<ImageData> images;
    private String user_locale;

    @Data
    public static class ImageData {
        private Metadata metadata;

        @Data
        public static class Metadata {
            private String time;
            private Location location;

            @Data
            public static class Location {
                private String administrative_area;
                private List<String> landmarks;
            }
        }
    }
}
