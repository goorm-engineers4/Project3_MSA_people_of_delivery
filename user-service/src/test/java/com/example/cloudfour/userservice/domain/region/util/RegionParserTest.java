package com.example.cloudfour.userservice.domain.region.util;

import com.example.cloudfour.userservice.domain.region.util.RegionParser.RegionParts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RegionParserTest {

    @Test
    @DisplayName("서울특별시 강남구 역삼동 → (시, 구, 동) 파싱")
    void parse_seoul_gangnam_yeoksam() {
        String addr = "서울특별시 강남구 역삼동 123-45";
        RegionParts parts = RegionParser.parseOrThrow(addr);
        assertThat(parts.si()).isEqualTo("서울특별시");
        assertThat(parts.gu()).isEqualTo("강남구");
        assertThat(parts.dong()).isEqualTo("역삼동");
    }

    @Test
    @DisplayName("경기도 성남시 분당구 정자동 → (도, 구, 동) 파싱")
    void parse_gyeonggi_seongnam_bundang_jeongja() {
        String addr = "경기도 성남시 분당구 정자동 27";
        RegionParts parts = RegionParser.parseOrThrow(addr);
        assertThat(parts.si()).isEqualTo("경기도");
        assertThat(parts.gu()).isEqualTo("분당구");
        assertThat(parts.dong()).isEqualTo("정자동");
    }

    @Test
    @DisplayName("괄호 내용 제거 후 파싱")
    void parse_ignores_parentheses() {
        String addr = "서울특별시 강남구 역삼동(2) 123";
        RegionParts parts = RegionParser.parseOrThrow(addr);
        assertThat(parts.si()).isEqualTo("서울특별시");
        assertThat(parts.gu()).isEqualTo("강남구");
        assertThat(parts.dong()).isEqualTo("역삼동");
    }

    @Test
    @DisplayName("도로명/번지 등 잡토큰이 있어도 동까지 찾으면 파싱")
    void parse_ignores_roadname_tail() {
        String addr = "서울특별시 강남구 역삼동 테헤란로 212";
        RegionParts parts = RegionParser.parseOrThrow(addr);
        assertThat(parts.si()).isEqualTo("서울특별시");
        assertThat(parts.gu()).isEqualTo("강남구");
        assertThat(parts.dong()).isEqualTo("역삼동");
    }

    @Test
    @DisplayName("형식이 부족하면 예외")
    void parse_invalid_throws() {
        assertThatThrownBy(() -> RegionParser.parseOrThrow("대한민국 서울 강남"))
                .isInstanceOf(RuntimeException.class);
    }
}
