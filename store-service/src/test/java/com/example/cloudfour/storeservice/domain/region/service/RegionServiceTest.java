package com.example.cloudfour.storeservice.domain.region.service;

import com.example.cloudfour.storeservice.domain.region.entity.Region;
import com.example.cloudfour.storeservice.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionService 단위테스트")
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    private UUID regionId;
    private Region region;

    @BeforeEach
    void setUp() {
        regionId = UUID.randomUUID();
        
        // Set up Region
        region = mock(Region.class);
        lenient().when(region.getId()).thenReturn(regionId);
        lenient().when(region.getSiDo()).thenReturn("서울시");
        lenient().when(region.getSiGunGu()).thenReturn("강남구");
        lenient().when(region.getEupMyeonDong()).thenReturn("역삼동");
    }

    @Nested
    @DisplayName("parseAndSaveRegion 메소드는")
    class ParseAndSaveRegionTests {

        @Test
        @DisplayName("유효한 주소가 주어지면 기존 지역을 찾아 ID를 반환한다")
        void parseAndSaveRegion_ValidAddress_ExistingRegion_ReturnsId() {
            // Given
            String address = "서울시 강남구 역삼동";
            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동"))
                    .thenReturn(Optional.of(region));

            // When
            UUID result = regionService.parseAndSaveRegion(address);

            // Then
            assertThat(result).isEqualTo(regionId);
            verify(regionRepository).findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동");
            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("유효한 주소가 주어지고 지역이 존재하지 않으면 새 지역을 생성하고 ID를 반환한다")
        void parseAndSaveRegion_ValidAddress_NewRegion_CreatesAndReturnsId() {
            // Given
            String address = "서울시 강남구 역삼동";
            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동"))
                    .thenReturn(Optional.empty());
            when(regionRepository.save(any(Region.class))).thenReturn(region);

            // When
            UUID result = regionService.parseAndSaveRegion(address);

            // Then
            assertThat(result).isEqualTo(regionId);
            verify(regionRepository).findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동");
            verify(regionRepository).save(any(Region.class));
        }

        @Test
        @DisplayName("유효하지 않은 주소 형식이 주어지면 예외를 던진다")
        void parseAndSaveRegion_InvalidAddress_ThrowsException() {
            // Given
            String invalidAddress = "서울시 강남구";

            // When & Then
            assertThatThrownBy(() -> regionService.parseAndSaveRegion(invalidAddress))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주소 형식이 올바르지 않습니다");
            
            verify(regionRepository, never()).findBySiDoAndSiGunGuAndEupMyeonDong(anyString(), anyString(), anyString());
            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("빈 주소가 주어지면 예외를 던진다")
        void parseAndSaveRegion_EmptyAddress_ThrowsException() {
            // Given
            String emptyAddress = "";

            // When & Then
            assertThatThrownBy(() -> regionService.parseAndSaveRegion(emptyAddress))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주소 형식이 올바르지 않습니다");
            
            verify(regionRepository, never()).findBySiDoAndSiGunGuAndEupMyeonDong(anyString(), anyString(), anyString());
            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("null 주소가 주어지면 예외를 던진다")
        void parseAndSaveRegion_NullAddress_ThrowsException() {
            // Given
            String nullAddress = null;

            // When & Then
            assertThatThrownBy(() -> regionService.parseAndSaveRegion(nullAddress))
                    .isInstanceOf(NullPointerException.class);
            
            verify(regionRepository, never()).findBySiDoAndSiGunGuAndEupMyeonDong(anyString(), anyString(), anyString());
            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("공백이 포함된 주소를 올바르게 처리한다")
        void parseAndSaveRegion_AddressWithExtraSpaces_ProcessesCorrectly() {
            // Given
            String addressWithExtraSpaces = "  서울시  강남구  역삼동  ";
            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동"))
                    .thenReturn(Optional.of(region));

            // When
            UUID result = regionService.parseAndSaveRegion(addressWithExtraSpaces);

            // Then
            assertThat(result).isEqualTo(regionId);
            verify(regionRepository).findBySiDoAndSiGunGuAndEupMyeonDong("서울시", "강남구", "역삼동");
            verify(regionRepository, never()).save(any(Region.class));
        }
    }
}