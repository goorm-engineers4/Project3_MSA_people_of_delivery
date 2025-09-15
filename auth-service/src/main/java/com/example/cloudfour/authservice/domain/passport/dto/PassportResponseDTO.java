package com.example.cloudfour.authservice.domain.passport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
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
