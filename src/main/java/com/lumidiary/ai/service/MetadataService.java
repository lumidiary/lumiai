package com.lumidiary.ai.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.lumidiary.ai.dto.Landmark;
import com.lumidiary.ai.dto.Location;
import com.lumidiary.ai.dto.Metadata;
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
import java.util.TimeZone;

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
            
            // GPS 정보 추출
            GpsDirectory gps = meta.getFirstDirectoryOfType(GpsDirectory.class);
            TimeZone timeZone = TimeZone.getDefault();
            
            if (gps != null && gps.getGeoLocation() != null) {
                double latitude = gps.getGeoLocation().getLatitude();
                double longitude = gps.getGeoLocation().getLongitude();
                
                // 좌표 기반 시간대 가져오기
                timeZone = getTimeZoneFromCoordinates(latitude, longitude);
                
                Location loc = Location.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();
                // 주소 포함
                String address = getAddressFromCoordinates(loc.getLatitude(), loc.getLongitude());
                loc = Location.builder()
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .address(address)
                        .build();
                builder.location(loc);
                // 랜드마크 포함 (반경 250)
                List<Landmark> landmarks = getNearbyLandmarks(loc.getLatitude(), loc.getLongitude(), 250);
                builder.nearbyLandmarks(landmarks);
            }
            
            // 날짜 추출 - 위에서 결정된 시간대 사용
            ExifSubIFDDirectory subDir = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subDir != null) {
                Date date = subDir.getDateOriginal(timeZone);
                if (date != null) {
                    String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                    builder.captureDate(formatted);
                }
            }
        }
        return builder.build();
    }
    
    // 좌표로부터 시간대 정보를 가져오는 메소드
    private TimeZone getTimeZoneFromCoordinates(double lat, double lng) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/timezone/json")
                .queryParam("location", lat + "," + lng)
                .queryParam("timestamp", System.currentTimeMillis() / 1000)
                .queryParam("key", googleMapsApiKey)
                .toUriString();
                
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("timeZoneId")) {
                String timeZoneId = response.get("timeZoneId").asText();
                return TimeZone.getTimeZone(timeZoneId);
            }
        } catch (Exception e) {
            System.err.println("시간대 조회 실패: " + e.getMessage());
        }
        
        return TimeZone.getDefault();
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
