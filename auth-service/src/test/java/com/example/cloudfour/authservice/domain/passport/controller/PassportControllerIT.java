package com.example.cloudfour.authservice.domain.passport.controller;

import com.example.cloudfour.authservice.domain.passport.dto.PassportRequestDTO;
import com.example.cloudfour.authservice.domain.passport.dto.PassportResponseDTO;
import com.example.cloudfour.authservice.domain.passport.service.PassportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PassportController 통합 테스트")
class PassportControllerIT {

    @InjectMocks PassportController controller;
    @Mock PassportService passportService;

    MockMvc mvc;
    ObjectMapper om;

    UUID userId;
    String role;
    String passportId;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        userId = UUID.randomUUID();
        role = "USER";
        passportId = "passport-123";
    }

    @Nested
    @DisplayName("Passport 생성")
    class CreatePassport {

        @Test
        @DisplayName("POST /api/passports: Passport 생성 성공")
        void createPassport_success() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(userId)
                    .role(role)
                    .build();

            var response = PassportResponseDTO.builder()
                    .passportId(passportId)
                    .userId(userId)
                    .role(role)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .authLevel("STANDARD")
                    .passportData("encrypted-data")
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenReturn(response);

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.passportId").value(passportId))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.role").value(role))
                    .andExpect(jsonPath("$.authLevel").value("STANDARD"))
                    .andExpect(jsonPath("$.passportData").value("encrypted-data"));

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }

        @Test
        @DisplayName("POST /api/passports: PassportService 예외 발생 시 400 반환")
        void createPassport_serviceException_400() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(userId)
                    .role(role)
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenThrow(new RuntimeException("Passport 생성 실패"));

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }

        @Test
        @DisplayName("POST /api/passports: 잘못된 요청 데이터 시 400 반환")
        void createPassport_invalidRequest_400() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(null)
                    .role(role)
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenThrow(new RuntimeException("Invalid request"));

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }

        @Test
        @DisplayName("POST /api/passports: 빈 role로 요청 시 400 반환")
        void createPassport_emptyRole_400() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(userId)
                    .role("")
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenThrow(new RuntimeException("Empty role"));

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("Passport 검증")
    class ValidatePassport {

        @Test
        @DisplayName("GET /api/passports/{passportId}/validate: 유효한 Passport 검증 성공")
        void validatePassport_valid_success() throws Exception {
            when(passportService.validatePassport(passportId))
                    .thenReturn(true);

            mvc.perform(get("/api/passports/{passportId}/validate", passportId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(passportService).validatePassport(passportId);
        }

        @Test
        @DisplayName("GET /api/passports/{passportId}/validate: 무효한 Passport 검증 시 false 반환")
        void validatePassport_invalid_false() throws Exception {
            when(passportService.validatePassport(passportId))
                    .thenReturn(false);

            mvc.perform(get("/api/passports/{passportId}/validate", passportId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(passportService).validatePassport(passportId);
        }

        @Test
        @DisplayName("GET /api/passports/{passportId}/validate: 존재하지 않는 Passport 검증 시 false 반환")
        void validatePassport_notFound_false() throws Exception {
            String nonExistentPassportId = "non-existent-passport";
            
            when(passportService.validatePassport(nonExistentPassportId))
                    .thenReturn(false);

            mvc.perform(get("/api/passports/{passportId}/validate", nonExistentPassportId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(passportService).validatePassport(nonExistentPassportId);
        }

        @Test
        @DisplayName("GET /api/passports/{passportId}/validate: 빈 passportId로 요청 시 404 반환")
        void validatePassport_emptyPassportId_404() throws Exception {
            mvc.perform(get("/api/passports//validate"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/passports/{passportId}/validate: 특수문자가 포함된 passportId 검증")
        void validatePassport_specialCharacters_false() throws Exception {
            String specialPassportId = "passport-123!@#";
            
            when(passportService.validatePassport(specialPassportId))
                    .thenReturn(false);

            mvc.perform(get("/api/passports/{passportId}/validate", specialPassportId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(passportService).validatePassport(specialPassportId);
        }
    }

    @Nested
    @DisplayName("다양한 역할 테스트")
    class DifferentRoles {

        @Test
        @DisplayName("POST /api/passports: OWNER 역할로 Passport 생성")
        void createPassport_ownerRole_success() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(userId)
                    .role("OWNER")
                    .build();

            var response = PassportResponseDTO.builder()
                    .passportId(passportId)
                    .userId(userId)
                    .role("OWNER")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .authLevel("PREMIUM")
                    .passportData("encrypted-data")
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenReturn(response);

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("OWNER"))
                    .andExpect(jsonPath("$.authLevel").value("PREMIUM"));

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }

        @Test
        @DisplayName("POST /api/passports: ADMIN 역할로 Passport 생성")
        void createPassport_adminRole_success() throws Exception {
            var request = PassportRequestDTO.builder()
                    .userId(userId)
                    .role("ADMIN")
                    .build();

            var response = PassportResponseDTO.builder()
                    .passportId(passportId)
                    .userId(userId)
                    .role("ADMIN")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .authLevel("ADMIN")
                    .passportData("encrypted-data")
                    .build();

            when(passportService.createPassport(any(PassportRequestDTO.class)))
                    .thenReturn(response);

            mvc.perform(post("/api/passports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.authLevel").value("ADMIN"));

            verify(passportService).createPassport(any(PassportRequestDTO.class));
        }
    }
}
