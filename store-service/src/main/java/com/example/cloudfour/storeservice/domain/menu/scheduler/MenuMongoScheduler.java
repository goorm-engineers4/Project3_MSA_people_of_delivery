package com.example.cloudfour.storeservice.domain.menu.scheduler;

import com.example.cloudfour.storeservice.domain.collection.repository.command.MenuCommandRepository;
import com.example.cloudfour.storeservice.domain.collection.repository.command.StockCommandRepository;
import com.example.cloudfour.storeservice.domain.common.enums.SyncStatus;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.entity.MenuOption;
import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuOptionRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MenuMongoScheduler {

    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final MenuCommandRepository menuCommandRepository;
    private final StockRepository stockRepository;
    private final StockCommandRepository stockCommandRepository;

    @Scheduled(cron = "0 0 4 * * *")
    public void createMenu(){
        log.info("MongoDB에 메뉴 동기화 시작");
        List<Menu> unsyncedMenus = menuRepository.findAllBySyncStatus(SyncStatus.CREATED_PENDING);
        if (unsyncedMenus.isEmpty()) {
            log.info("MongoDB에 생성할 메뉴가 존재하지 않음");
            return;
        }
        Map<UUID, List<Menu>> menusByStoreId = unsyncedMenus.stream()
                .collect(Collectors.groupingBy(menu->menu.getStore().getId()));

        List<UUID> storeIds = new ArrayList<>(menusByStoreId.keySet());
        menuCommandRepository.createMenuByStoreId(storeIds, menusByStoreId);
        unsyncedMenus.forEach(Menu::syncCreated);
        menuRepository.saveAll(unsyncedMenus);
        log.info("MongoDB에 메뉴 동기화 완료");
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void createMenuOption(){
        log.info("MongoDB에 메뉴옵션 동기화 시작");
        List<MenuOption> unsyncedMenuOptions = menuOptionRepository.findAllBySyncStatus(SyncStatus.CREATED_PENDING);
        if(unsyncedMenuOptions.isEmpty()){
            log.info("MongoDB에 생성할 메뉴옵션이 존재하지 않음");
            return;
        }
        Map<UUID, List<MenuOption>> menuOptionsByMenuId = unsyncedMenuOptions.stream()
                .collect(Collectors.groupingBy(menuOption->menuOption.getMenu().getId()));

        List<UUID> menuIds = new ArrayList<>(menuOptionsByMenuId.keySet());
        menuCommandRepository.createMenuOptionByMenuId(menuIds, menuOptionsByMenuId);
        unsyncedMenuOptions.forEach(MenuOption::syncCreated);
        menuOptionRepository.saveAll(unsyncedMenuOptions);
        log.info("MongoDB에 메뉴 옵션 동기화 완료");
    }

    @Scheduled(cron = "0 * * * * *")
    public void deleteMenu(){
        log.info("MongoDB에 메뉴 삭제 동기화 시작");
        List<Menu> menus = menuRepository.findAllByIsDeleted();
        if(menus.isEmpty()){
            log.info("삭제할 메뉴 데이터 없음");
            return;
        }
        List<UUID> menuIds = menus.stream().map(Menu::getId).toList();
        menuCommandRepository.deleteAllByMenuIdIn(menuIds);
        log.info("MongoDB에 메뉴 삭제 완료");
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void deleteMenuOption(){
        log.info("MongoDB에 메뉴 옵션 삭제 동기화 시작");
        List<MenuOption> menuOptions = menuOptionRepository.findAllByIsDeleted();
        if(menuOptions.isEmpty()){
            log.info("삭제할 메뉴 옵션 데이터 없음");
            return;
        }
        List<UUID> menuOptionIds = menuOptions.stream().map(MenuOption::getId).toList();
        menuCommandRepository.deleteAllByMenuOptionIdIn(menuOptionIds);
        log.info("MongoDB에 메뉴 옵션 삭제 완료");
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void refreshQuantity(){
        log.info("MongoDB에 수량 최신화 시작");
        List<Stock> pendingStocks = stockRepository.findAllBySyncStatus(SyncStatus.UPDATED_PENDING);
        if(pendingStocks.isEmpty()){
            log.info("MongoDB에 최신화할 수량아 존재하지 않음");
        }

        for(Stock stock: pendingStocks){
            stockCommandRepository.updateStockByMenuId(stock.getMenu().getId(), stock.getQuantity());
            stock.setSyncStatus(SyncStatus.UPDATED_SYNCED);
            stockRepository.save(stock);
        }
        log.info("MongoDB에 수량 최신화 완료");
    }
}
