package com.example.cloudfour.storeservice.domain.region.repository;

import com.example.cloudfour.storeservice.domain.region.entity.Region;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.example.cloudfour.storeservice.domain.region.entity.QRegion.region;

@Repository
@RequiredArgsConstructor
public class RegionQueryDslRepositoryImpl implements RegionQueryDslRepository {
    private final JPAQueryFactory query;

    @Override
    public Optional<Region> findBySiDoAndSiGunGuAndEupMyeonDong(String siDo, String siGunGu, String eupMyeonDong) {
        BooleanBuilder regionBuilder = new BooleanBuilder();
        if (eupMyeonDong != null) regionBuilder.or(region.eupMyeonDong.eq(eupMyeonDong));
        if (siGunGu != null) regionBuilder.or(region.siGunGu.eq(siGunGu));
        if (siDo != null) regionBuilder.or(region.siDo.eq(siDo));

        return Optional.ofNullable(query.selectFrom(region).where(regionBuilder).fetchFirst());
    }
}
