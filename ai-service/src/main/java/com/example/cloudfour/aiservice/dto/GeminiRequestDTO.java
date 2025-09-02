package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeminiRequestDTO {
    private List<Content> contents;

    @Getter
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @Builder
    public static class Part {
        private String text;
    }
}