package com.example.cloudfour.userservice.domain.region.entity;

import com.example.cloudfour.userservice.domain.region.exception.RegionErrorCode;
import com.example.cloudfour.userservice.domain.region.exception.RegionException;
import com.example.cloudfour.userservice.domain.user.entity.UserAddress;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "p_region",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_region_sgd",
                columnNames = {"siDo","siGunGu","eupMyeonDong"}
        )
)
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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "region")
    private List<UserAddress> addresses = new ArrayList<>();

    public static Region ofRaw(String siDo, String siGunGu, String eupMyeonDong) {
        Region r = new Region();
        r.siDo = siDo;
        r.siGunGu = siGunGu;
        r.eupMyeonDong = eupMyeonDong;
        return r;
    }

}
