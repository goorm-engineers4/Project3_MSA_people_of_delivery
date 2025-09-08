package com.example.cloudfour.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassportResponseDTO {
    private String passportId;
    private UUID userId;
    private String role;
    private Instant issuedAt;
    private Instant expiresAt;
    private String authLevel;
    private String passportData;
}
