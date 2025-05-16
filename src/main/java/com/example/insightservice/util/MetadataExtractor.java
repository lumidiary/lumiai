package com.example.insightservice.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.example.insightservice.dto.Landmark;
import com.example.insightservice.dto.Location;
import com.example.insightservice.dto.Metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@RequiredArgsConstructor
public class MetadataExtractor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    public Metadata extractMetadata(MultipartFile imageFile, boolean includeAddress, boolean includeLandmarks, int landmarkRadius) throws Exception {
        Metadata.MetadataDTOBuilder metadataBuilder = extractBasicMetadata(imageFile);

        if (metadataBuilder.build().getLocation() != null) {
            Location location = metadataBuilder.build().getLocation();
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            if (includeAddress) {
                String address = getAddressFromCoordinates(lat, lng);
                Location updatedLocation = Location.builder()
                        .latitude(lat)
                        .longitude(lng)
                        .address(address)
                        .build();
                metadataBuilder.location(updatedLocation);
            }

            if (includeLandmarks) {
                List<Landmark> landmarks = getNearbyLandmarks(lat, lng, landmarkRadius);
                metadataBuilder.nearbyLandmarks(landmarks);
            }
        }

        return metadataBuilder.build();
    }

    public JsonNode extractAllMetadataTags(MultipartFile imageFile) throws Exception {
        ObjectNode resultNode = objectMapper.createObjectNode();
        try (InputStream input = imageFile.getInputStream()) {
            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(input);
            for (Directory directory : metadata.getDirectories()) {
                ObjectNode dirNode = objectMapper.createObjectNode();
                for (Tag tag : directory.getTags()) {
                    dirNode.put(tag.getTagName(), tag.getDescription());
                }
                resultNode.set(directory.getName(), dirNode);
            }
        }
        return resultNode;
    }

    private Metadata.MetadataDTOBuilder extractBasicMetadata(MultipartFile imageFile) throws Exception {
        Metadata.MetadataDTOBuilder builder = Metadata.builder();
        try (InputStream input = imageFile.getInputStream()) {
            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(input);
            builder.filename(imageFile.getOriginalFilename());
            
            // 날짜 추출
            ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir != null) {
                Date date = dir.getDateOriginal();
                if (date != null) {
                    String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                    builder.captureDate(formatted);
                }
            }
            
            // 위치 추출
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null && gps.getGeoLocation() != null) {
                Location location = Location.builder()
                        .latitude(gps.getGeoLocation().getLatitude())
                        .longitude(gps.getGeoLocation().getLongitude())
                        .build();
                builder.location(location);
            }
            
            // 장치 정보 추출
            ExifIFD0Directory deviceDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (deviceDir != null) {
                Metadata.DeviceInfoDTO deviceInfo = Metadata.DeviceInfoDTO.builder()
                        .make(deviceDir.getString(ExifIFD0Directory.TAG_MAKE))
                        .model(deviceDir.getString(ExifIFD0Directory.TAG_MODEL))
                        .software(deviceDir.getString(ExifIFD0Directory.TAG_SOFTWARE))
                        .build();
                builder.deviceInfo(deviceInfo);
            }
        }
        return builder;
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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
