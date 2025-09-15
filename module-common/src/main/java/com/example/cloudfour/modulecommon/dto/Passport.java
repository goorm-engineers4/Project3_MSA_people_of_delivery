package com.example.cloudfour.modulecommon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Passport {
    private String passportId;
    private UUID userId;
    private String role;
    private Instant issuedAt;
    private Instant expiresAt;
    private String authLevel;
    
    @JsonIgnore
    public boolean isValid() {
        return passportId != null && 
               userId != null && 
               role != null && 
               issuedAt != null && 
               expiresAt != null &&
               Instant.now().isBefore(expiresAt);
    }

    @JsonIgnore
    public boolean isHighAuthLevel() {
        return "HIGH".equals(authLevel);
    }
}
