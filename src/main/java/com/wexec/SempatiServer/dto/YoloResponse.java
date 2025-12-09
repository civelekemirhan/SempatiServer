package com.wexec.SempatiServer.dto;

import lombok.Data;
import java.util.List;

@Data
public class YoloResponse {
    private boolean success;
    private String type; // "image" veya "video"
    private boolean contains_animal; // true/false
    private String message;
    private List<Detection> detections;

    @Data
    public static class Detection {
        private String type; // "cat", "dog"
        private double confidence; // 0.85
        private List<Double> bbox; // Koordinatlar
    }
}