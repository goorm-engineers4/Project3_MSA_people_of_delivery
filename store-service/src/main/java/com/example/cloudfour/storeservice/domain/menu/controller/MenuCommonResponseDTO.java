package com.example.cloudfour.storeservice.domain.menu.controller;
import com.example.cloudfour.storeservice.domain.menu.enums.MenuStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MenuCommonResponseDTO {
    private UUID menuId;
    private String name;
    private Integer price;
    private String menuPicture;
    private MenuStatus status;
    private String category;
}
