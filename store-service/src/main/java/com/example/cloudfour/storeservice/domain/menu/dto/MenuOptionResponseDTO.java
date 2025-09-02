package com.example.cloudfour.storeservice.domain.menu.dto;

import com.example.cloudfour.storeservice.domain.menu.controller.MenuOptionCommonResponseDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

public class MenuOptionResponseDTO {
    @Getter
    @Builder
    public static class MenuOptionDetailResponseDTO {
        @JsonUnwrapped
        MenuOptionCommonResponseDTO menuOptionCommonResponseDTO;
    }

    @Getter
    @Builder
    public static class MenuOptionsByMenuResponseDTO {
        private List<MenuOptionSimpleResponseDTO> options;
    }

    @Getter
    @Builder
    public static class MenuOptionSimpleResponseDTO {
        private UUID menuOptionId;
        private String optionName;
        private Integer additionalPrice;
    }
}
