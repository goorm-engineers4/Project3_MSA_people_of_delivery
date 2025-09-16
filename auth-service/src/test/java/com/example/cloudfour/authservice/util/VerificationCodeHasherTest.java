package com.example.cloudfour.authservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VerificationCodeHasher 단위 테스트")
class VerificationCodeHasherTest {

    private static void setPepper(VerificationCodeHasher hasher, String pepper) throws Exception {
        Field f = VerificationCodeHasher.class.getDeclaredField("pepper");
        f.setAccessible(true);
        f.set(hasher, pepper);
    }

    private static String expected(String plain, String pepper) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update((plain + pepper).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(md.digest());
    }

    @Test
    @DisplayName("같은 페퍼에서는 동일 입력에 동일 해시")
    void deterministic_with_same_pepper() throws Exception {
        var hasher = new VerificationCodeHasher();
        setPepper(hasher, "pep1");

        String out1 = hasher.hash("123456");
        String out2 = hasher.hash("123456");

        assertThat(out1).isEqualTo(out2);
        assertThat(out1).isEqualTo(expected("123456", "pep1"));
    }

    @Test
    @DisplayName("페퍼 변경 시 해시도 변경")
    void changes_with_pepper() throws Exception {
        var hasher = new VerificationCodeHasher();
        setPepper(hasher, "pep1");
        String a = hasher.hash("654321");

        setPepper(hasher, "pep2");
        String b = hasher.hash("654321");

        assertThat(a).isNotEqualTo(b);
        assertThat(b).isEqualTo(expected("654321", "pep2"));
    }
}

