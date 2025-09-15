package com.example.cloudfour.storeservice.domain.menu.repository.querydsl;

import com.example.cloudfour.storeservice.domain.menu.entity.MenuOption;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.cloudfour.storeservice.domain.menu.entity.QMenu.menu;
import static com.example.cloudfour.storeservice.domain.menu.entity.QMenuOption.menuOption;

@Repository
@RequiredArgsConstructor
public class MenuOptionQueryDslRepositoryImpl implements MenuOptionQueryDslRepository{

    private final JPAQueryFactory query;

    @Override
    public Optional<MenuOption> findByIdWithMenu(UUID optionId) {
        return Optional.ofNullable(query.selectFrom(menuOption).join(menuOption.menu,menu).fetchJoin()
                .where(menuOption.id.eq((optionId))).fetchOne());
    }

    @Override
    public boolean existsByMenuIdAndOptionName(UUID menuId, String optionName) {
        return query.selectFrom(menuOption).join(menuOption.menu,menu).fetchJoin()
                .where(menu.id.eq(menuId), menuOption.optionName.eq(optionName)).fetchFirst() != null;
    }
}
