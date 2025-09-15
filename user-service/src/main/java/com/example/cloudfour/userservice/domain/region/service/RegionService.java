package com.example.cloudfour.userservice.domain.region.service;

import com.example.cloudfour.userservice.domain.region.dto.RegionResponseDTO;
import com.example.cloudfour.userservice.domain.region.entity.Region;
import com.example.cloudfour.userservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.userservice.domain.region.exception.RegionException;
import com.example.cloudfour.userservice.domain.region.repository.RegionRepository;
import com.example.cloudfour.userservice.domain.region.util.RegionParser;
import com.example.cloudfour.userservice.domain.user.entity.UserAddress;
import com.example.cloudfour.userservice.domain.user.exception.UserAddressErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserAddressException;
import com.example.cloudfour.userservice.domain.user.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;
    private final UserAddressRepository userAddressRepository;

    @Transactional
    public Region getOrCreateFromAddress(String fullAddress) {
        var p = RegionParser.parseOrThrow(fullAddress);
        var n = normalize(p.si(), p.gu(), p.dong());
        return getOrCreate(n.siDo, n.siGunGu, n.eupMyeonDong);
    }

    @Transactional
    public Region getOrCreate(String siDo, String siGunGu, String eupMyeonDong) {
        var n = normalize(siDo, siGunGu,eupMyeonDong);
        return regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong(n.siDo, n.siGunGu, n.eupMyeonDong)
                .orElseGet(() -> insertOrFind(n.siDo, n.siGunGu, n.eupMyeonDong));
    }

    private Region insertOrFind(String siDo, String siGunGu, String eupMyeonDong) {
        try {
            Region saved = regionRepository.save(Region.ofRaw(siDo, siGunGu,eupMyeonDong));
            regionRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            return regionRepository.findBySiDoAndSiGunGuAndEupMyeonDong(siDo, siGunGu,eupMyeonDong)
                    .orElseThrow(() -> e);
        }
    }

    private record Sgd(String siDo, String siGunGu, String eupMyeonDong) {}

    private static Sgd normalize(String siDo, String siGunGu, String eupMyeonDong) {
        return new Sgd(nn(siDo), nn(siGunGu), nn(eupMyeonDong));
    }
    private static String nn(String v) {
        if (v == null) throw new RegionException(RegionErrorCode.INTERNAL_ERROR);
        String out = v.trim().replaceAll("\\s+", " ");
        if (out.isEmpty()) throw new RegionException(RegionErrorCode.INTERNAL_ERROR);
        return out;
    }

    public RegionResponseDTO getRegion(UUID userId){
        UserAddress findUserAddress = userAddressRepository.findPrimaryByIdAndUserId(userId)
                .orElseThrow(()->new UserAddressException(UserAddressErrorCode.NOT_FOUND));
        Region region = findUserAddress.getRegion();
        return RegionResponseDTO.builder()
                .siDo(region.getSiDo())
                .eupMyeonDong(region.getEupMyeonDong())
                .siGunGu(region.getSiGunGu())
                .build();
    }
}


