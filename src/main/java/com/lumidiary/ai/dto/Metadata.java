package com.lumidiary.ai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    private String captureDate;
    private Location location;
    private List<Landmark> nearbyLandmarks;
}
