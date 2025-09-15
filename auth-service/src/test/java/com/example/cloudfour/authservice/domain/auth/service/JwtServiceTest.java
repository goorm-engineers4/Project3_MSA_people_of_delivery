package com.example.cloudfour.authservice.domain.auth.service;

import com.example.cloudfour.authservice.properties.JwtProps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService 단위 테스트")
class JwtServiceTest {

    @InjectMocks JwtService sut;
    @Mock JwtEncoder encoder;
    @Mock JwtDecoder decoder;
    @Mock JwtProps props;

    UUID uid;

    @BeforeEach
    void setUp() {
        uid = UUID.randomUUID();
    }

    @Test
    @DisplayName("accessTtlSeconds(): 설정값 그대로 반환")
    void access_ttl_returns_value() {
        when(props.getAccessExpSeconds()).thenReturn(3600L);
        assertThat(sut.accessTtlSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("refreshTtlSeconds(): 설정값 그대로 반환")
    void refresh_ttl_returns_value() {
        when(props.getRefreshExpSeconds()).thenReturn(7200L);
        assertThat(sut.refreshTtlSeconds()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("createRefresh(): Encoder 호출 후 토큰값 반환")
    void createRefresh_returns_token() {
        // props는 이 테스트에서만 필요
        when(props.getIssuer()).thenReturn("issuer");
        when(props.getAudience()).thenReturn("aud");
        when(props.getRefreshExpSeconds()).thenReturn(7200L);
        JwtProps.Jwks jwks = mock(JwtProps.Jwks.class);
        when(jwks.getKeyId()).thenReturn("kid");
        when(props.getJwks()).thenReturn(jwks);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("TOK");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String tok = sut.createRefresh(uid, "USER");
        assertThat(tok).isEqualTo("TOK");
        verify(encoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    @DisplayName("isValid(): decode 성공/실패 케이스")
    void isValid_cases() {
        when(decoder.decode("ok")).thenReturn(mock(Jwt.class));
        assertThat(sut.isValid("ok")).isTrue();

        when(decoder.decode("bad")).thenThrow(new JwtException("x"));
        assertThat(sut.isValid("bad")).isFalse();
    }

    @Test
    @DisplayName("userId()/role(): 디코딩된 JWT에서 추출")
    void extract_userId_and_role() {
        Jwt jwt = Jwt.withTokenValue("v")
                .subject(uid.toString())
                .claim("role", "ADMIN")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(decoder.decode("t")).thenReturn(jwt);

        assertThat(sut.userId("t")).isEqualTo(uid.toString());
        assertThat(sut.role("t")).isEqualTo("ADMIN");
    }
}
