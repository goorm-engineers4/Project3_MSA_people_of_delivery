package com.example.cloudfour.aiservice.commondto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponseDTO {
    private UUID userId;
    private String email;
    private String nickname;
    private String number;
    private String role;
    private String loginType;
    private boolean emailVerified;
}