package com.example.insightservice.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.example.insightservice.dto.Landmark;
import com.example.insightservice.dto.Location;
import com.example.insightservice.dto.Metadata;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final RestTemplate restTemplate;

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    // Modified method: accepts image bytes instead of MultipartFile
    public Metadata extractMetadata(byte[] imageBytes) throws Exception {
        Metadata.MetadataBuilder builder = Metadata.builder();
        try (InputStream input = new ByteArrayInputStream(imageBytes)) {
            com.drew.metadata.Metadata meta = ImageMetadataReader.readMetadata(input);
            // 날짜 추출
            ExifSubIFDDirectory subDir = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subDir != null) {
                Date date = subDir.getDateOriginal();
                if (date != null) {
                    String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                    builder.captureDate(formatted);
                }
            }
            // 위치 추출
            GpsDirectory gps = meta.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null && gps.getGeoLocation() != null) {
                Location loc = Location.builder()
                        .latitude(gps.getGeoLocation().getLatitude())
                        .longitude(gps.getGeoLocation().getLongitude())
                        .build();
                // 항상 주소 포함
                String address = getAddressFromCoordinates(loc.getLatitude(), loc.getLongitude());
                loc = Location.builder()
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .address(address)
                        .build();
                builder.location(loc);
                // 항상 랜드마크 포함 (반경 1000)
                List<Landmark> landmarks = getNearbyLandmarks(loc.getLatitude(), loc.getLongitude(), 1000);
                builder.nearbyLandmarks(landmarks);
            }
        }
        return builder.build();
    }
    
    private String getAddressFromCoordinates(double lat, double lng) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", lat + "," + lng)
                .queryParam("key", googleMapsApiKey)
                .toUriString();
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response.has("results") && response.get("results").size() > 0) {
                return response.get("results").get(0).get("formatted_address").asText();
            }
        } catch (Exception e) {
            return "주소 조회 실패: " + e.getMessage();
        }
        return "주소 정보 없음";
    }
    
    private List<Landmark> getNearbyLandmarks(double lat, double lng, int radius) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radius)
                .queryParam("fields", "place_id,name,vicinity")
                .queryParam("key", googleMapsApiKey)
                .toUriString();
        List<Landmark> landmarks = new ArrayList<>();
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response.has("results")) {
                for (JsonNode result : response.get("results")) {
                    Landmark landmark = Landmark.builder()
                            .id(result.has("place_id") ? result.get("place_id").asText() : null)
                            .name(result.has("name") ? result.get("name").asText() : null)
                            .build();
                    landmarks.add(landmark);
                }
            }
        } catch (Exception e) {
            landmarks.add(Landmark.builder()
                    .id("error")
                    .name("조회 실패: " + e.getMessage())
                    .build());
        }
        return landmarks;
    }
}
