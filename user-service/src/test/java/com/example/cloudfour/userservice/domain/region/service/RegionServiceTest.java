package com.example.cloudfour.userservice.domain.region.service;

import com.example.cloudfour.userservice.domain.region.entity.Region;
import com.example.cloudfour.userservice.domain.region.repository.RegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @InjectMocks RegionService sut;
    @Mock RegionRepository regionRepository;

    private Region dummyRegion() {
        return mock(Region.class);
    }

    @Nested
    @DisplayName("getOrCreateFromAddress(String)")
    class GetOrCreateFromAddress {

        @Test
        @DisplayName("존재하면 저장 없이 기존 Region 반환")
        void return_existing_without_save() {
            String addr = "서울특별시 강남구 역삼동 123";
            Region existing = dummyRegion();

            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("서울특별시","강남구","역삼동"))
                    .thenReturn(Optional.of(existing));

            Region out = sut.getOrCreateFromAddress(addr);

            assertThat(out).isSameAs(existing);
            verify(regionRepository).findBySiDoAndSiGunGuAndEupMyeonDong("서울특별시","강남구","역삼동");
            verify(regionRepository, never()).save(any());
        }

        @Test
        @DisplayName("없으면 저장 후 반환")
        void save_when_absent() {
            String addr = "경기도 성남시 분당구 정자동 27";
            Region saved = dummyRegion();

            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("경기도","분당구","정자동"))
                    .thenReturn(Optional.empty());
            when(regionRepository.save(any(Region.class))).thenReturn(saved);

            Region out = sut.getOrCreateFromAddress(addr);

            assertThat(out).isSameAs(saved);
            verify(regionRepository).save(any(Region.class));
        }

        @Test
        @DisplayName("Unique 충돌 시 재조회하여 기존 Region 반환")
        void save_conflict_fallback_to_find() {
            String addr = "서울특별시 송파구 잠실동 1";
            Region existing = dummyRegion();

            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("서울특별시","송파구","잠실동"))
                    .thenReturn(Optional.empty(), Optional.of(existing));
            when(regionRepository.save(any(Region.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            Region out = sut.getOrCreateFromAddress(addr);

            assertThat(out).isSameAs(existing);
            verify(regionRepository, times(2))
                    .findBySiDoAndSiGunGuAndEupMyeonDong("서울특별시","송파구","잠실동");
            verify(regionRepository).save(any(Region.class));
        }
    }

    @Nested
    @DisplayName("getOrCreate(siDo, siGunGu, eupMyeonDong)")
    class GetOrCreate {

        @Test
        @DisplayName("존재 시 그대로 반환")
        void return_existing() {
            Region existing = dummyRegion();

            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("부산광역시","해운대구","좌동"))
                    .thenReturn(Optional.of(existing));

            Region out = sut.getOrCreate("부산광역시","해운대구","좌동");

            assertThat(out).isSameAs(existing);
            verify(regionRepository, never()).save(any());
        }

        @Test
        @DisplayName("없으면 저장 후 반환")
        void save_when_absent() {
            Region saved = dummyRegion();

            when(regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong("대구광역시","수성구","범어동"))
                    .thenReturn(Optional.empty());
            when(regionRepository.save(any(Region.class))).thenReturn(saved);

            Region out = sut.getOrCreate("대구광역시","수성구","범어동");

            assertThat(out).isSameAs(saved);
            verify(regionRepository).save(any(Region.class));
        }
    }
}
