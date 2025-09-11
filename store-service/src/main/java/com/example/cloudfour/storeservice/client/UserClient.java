package com.example.cloudfour.storeservice.client;


import com.example.cloudfour.storeservice.config.FeignConfig;
import com.example.cloudfour.storeservice.domain.common.RegionResponseDTO;
import com.example.cloudfour.storeservice.domain.common.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name="user-service", path = "http://user-service.app.svc.cluster.local:80/internal",configuration = FeignConfig.class)
public interface UserClient {

    @GetMapping("/regions/{userId}")
    RegionResponseDTO getUserRegion(@PathVariable UUID userId);

    @GetMapping("/users/{userId}")
    UserResponseDTO getUser(@PathVariable UUID userId);
}
