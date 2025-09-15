package com.example.cloudfour.modulecommon.util;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Component
public class PassportUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String DEFAULT_SECRET_KEY = "default-secret-key-change-in-production";
    
    @Value("${passport.secret-key:default-secret-key-change-in-production}")
    private String secretKey;

    public String serialize(Passport passport) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(passport);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

            String sigB64 = signB64Url(payload);
            return payloadB64 + "." + sigB64;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Passport 직렬화 실패", e);
        }
    }

    private String signB64Url(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            String keyToUse = (secretKey != null) ? secretKey : DEFAULT_SECRET_KEY;
            mac.init(new SecretKeySpec(keyToUse.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] sig = mac.doFinal(data);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC 서명 생성 실패", e);
        }
    }

    public Passport deserialize(String passportData) {
        try {
            int dot = passportData.lastIndexOf('.');
            if (dot < 0) throw new RuntimeException("잘못된 Passport 형식");

            String payloadB64 = passportData.substring(0, dot);
            String sigB64 = passportData.substring(dot + 1);

            byte[] payload = Base64.getUrlDecoder().decode(payloadB64);
            byte[] givenSig = Base64.getUrlDecoder().decode(sigB64);
            byte[] expectSig = hmac(payload);

            // 타이밍 공격 방지: 상수시간 비교
            if (!java.security.MessageDigest.isEqual(expectSig, givenSig)) {
                throw new RuntimeException("Passport 서명 검증 실패");
            }
            return objectMapper.readValue(payload, Passport.class);
        } catch (Exception e) {
            log.error("Passport 역직렬화 실패: {}", passportData, e);
            throw new RuntimeException("Passport 역직렬화 실패", e);
        }
    }

    private byte[] hmac(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        String keyToUse = (secretKey != null) ? secretKey : DEFAULT_SECRET_KEY;
        mac.init(new SecretKeySpec(keyToUse.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        return mac.doFinal(data);
    }
    
    public Passport deserializeOrNull(String passportData) {
        try {
            return deserialize(passportData);
        } catch (Exception e) {
            log.warn("Passport 역직렬화 실패 (null 반환): {}", passportData);
            return null;
        }
    }
    
}
