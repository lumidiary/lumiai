package com.example.insightservice.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    public JsonNode extractMetadata(MultipartFile imageFile, boolean includeAddress, boolean includeLandmarks, int landmarkRadius) throws Exception {
        ObjectNode resultNode = extractBasicMetadata(imageFile);

        JsonNode locationNode = resultNode.get("location");
        if (locationNode != null && !locationNode.isNull()) {
            double lat = locationNode.get("latitude").asDouble();
            double lng = locationNode.get("longitude").asDouble();

            if (includeAddress) {
                String address = getAddressFromCoordinates(lat, lng);
                ((ObjectNode) locationNode).put("address", address);
            }

            if (includeLandmarks) {
                List<String> landmarks = getNearbyLandmarks(lat, lng, landmarkRadius);
                ArrayNode landmarksNode = objectMapper.createArrayNode();
                landmarks.forEach(landmarksNode::add);
                resultNode.set("nearbyLandmarks", landmarksNode);
            }
        }

        return resultNode;
    }

    public JsonNode extractAllMetadataTags(MultipartFile imageFile) throws Exception {
        ObjectNode resultNode = objectMapper.createObjectNode();
        try (InputStream input = imageFile.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(input);
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

    private ObjectNode extractBasicMetadata(MultipartFile imageFile) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();
        try (InputStream input = imageFile.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(input);
            result.put("filename", imageFile.getOriginalFilename());
            extractDateTime(metadata, result);
            extractLocation(metadata, result);
            extractDevice(metadata, result);
        }
        return result;
    }

    private void extractDateTime(Metadata metadata, ObjectNode result) {
        ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (dir != null) {
            Date date = dir.getDateOriginal();
            if (date != null) {
                String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                result.put("captureDate", formatted);
            }
        }
    }

    private void extractLocation(Metadata metadata, ObjectNode result) {
        GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null && gps.getGeoLocation() != null) {
            ObjectNode loc = objectMapper.createObjectNode();
            loc.put("latitude", gps.getGeoLocation().getLatitude());
            loc.put("longitude", gps.getGeoLocation().getLongitude());
            result.set("location", loc);
        }
    }

    private void extractDevice(Metadata metadata, ObjectNode result) {
        ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (dir != null) {
            ObjectNode device = objectMapper.createObjectNode();

            if (dir.containsTag(ExifIFD0Directory.TAG_MAKE))
                device.put("make", dir.getString(ExifIFD0Directory.TAG_MAKE));

            if (dir.containsTag(ExifIFD0Directory.TAG_MODEL))
                device.put("model", dir.getString(ExifIFD0Directory.TAG_MODEL));

            if (dir.containsTag(ExifIFD0Directory.TAG_SOFTWARE))
                device.put("software", dir.getString(ExifIFD0Directory.TAG_SOFTWARE));

            if (device.size() > 0)
                result.set("deviceInfo", device);
        }
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

    private List<String> getNearbyLandmarks(double lat, double lng, int radius) {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radius)
                .queryParam("key", googleMapsApiKey)
                .toUriString();

        List<String> landmarks = new ArrayList<>();

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response.has("results")) {
                for (JsonNode result : response.get("results")) {
                    landmarks.add(result.get("name").asText());
                }
            }
        } catch (Exception e) {
            landmarks.add("조회 실패: " + e.getMessage());
        }

        return landmarks;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
