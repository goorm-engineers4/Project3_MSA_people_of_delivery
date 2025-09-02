package com.example.cloudfour.storeservice.domain.region.entity;

import com.example.cloudfour.storeservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.storeservice.domain.region.exception.RegionException;
import com.example.cloudfour.storeservice.domain.store.entity.Store;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "p_region")
public class Region {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String siDo;

    @Column(nullable = false, length = 20)
    private String siGunGu;

    @Column(nullable = false, length = 50)
    private String eupMyeonDong;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "region", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Store> stores = new ArrayList<>();

    public static class RegionBuilder{
        private RegionBuilder id(UUID id){
            throw new RegionException(RegionErrorCode.CREATE_FAILED);
        }
    }
}
