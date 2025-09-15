package com.example.cloudfour.userservice.domain.region.controller;

import com.example.cloudfour.userservice.domain.region.dto.RegionResponseDTO;
import com.example.cloudfour.userservice.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/regions")
public class InternalRegionController {

    private final RegionService query;

    @GetMapping("/{userId}")
    public RegionResponseDTO getRegion(@PathVariable UUID userId){
        return query.getRegion(userId);
    }
}
