package com.example.cloudfour.authservice.domain.passport.controller;

import com.example.cloudfour.authservice.domain.passport.dto.PassportRequestDTO;
import com.example.cloudfour.authservice.domain.passport.dto.PassportResponseDTO;
import com.example.cloudfour.authservice.domain.passport.service.PassportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/passports")
@RequiredArgsConstructor
public class PassportController {
    
    private final PassportService passportService;
    
    @PostMapping
    public ResponseEntity<PassportResponseDTO> createPassport(@RequestBody PassportRequestDTO request) {
        log.info("Passport 생성 요청: userId={}, role={}", 
                request.getUserId(), request.getRole());
        
        try {
            PassportResponseDTO passport = passportService.createPassport(request);
            log.info("Passport 생성 완료: passportId={}", passport.getPassportId());
            return ResponseEntity.ok(passport);
        } catch (Exception e) {
            log.error("Passport 생성 실패: userId={}", request.getUserId(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{passportId}/validate")
    public ResponseEntity<Boolean> validatePassport(@PathVariable String passportId) {
        log.debug("Passport 검증 요청: passportId={}", passportId);
        
        boolean isValid = passportService.validatePassport(passportId);
        return ResponseEntity.ok(isValid);
    }
}
