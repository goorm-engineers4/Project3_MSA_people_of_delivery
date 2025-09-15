package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeminiResponseDTO {
    private List<Candidate> candidates;

    @Getter
    @Builder
    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
        private List<SafetyRating> safetyRatings;
    }

    @Getter
    @Builder
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Getter
    @Builder
    public static class Part {
        private String text;
    }

    @Getter
    @Builder
    public static class SafetyRating {
        private String category;
        private String probability;
    }
}