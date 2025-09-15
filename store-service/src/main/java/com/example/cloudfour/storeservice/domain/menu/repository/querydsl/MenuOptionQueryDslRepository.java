package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import com.example.cloudfour.storeservice.domain.menu.entity.MenuOption;

import java.util.Optional;
import java.util.UUID;

public interface MenuOptionQueryDslRepository {
    Optional<MenuOption> findByIdWithMenu(UUID optionId);

    boolean existsByMenuIdAndOptionName(UUID menuId, String optionName);
}
