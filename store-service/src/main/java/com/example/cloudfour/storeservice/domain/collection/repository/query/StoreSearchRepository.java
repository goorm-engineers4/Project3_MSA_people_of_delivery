package com.example.cloudfour.storeservice.domain.collection.repository.query;

import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreSearchRepository {
    Optional<StoreDocument> findStoreByStoreId(UUID storeId);

    Slice<StoreDocument> findAllStoreByCategoryAndCursor(UUID categoryId, LocalDateTime cursor, Pageable pageable
    , String siDo, String siGunGu, String eupMyeongDong);

    Slice<StoreDocument> findAllStoreByKeyWordAndRegion(String keyword, LocalDateTime cursor, Pageable pageable
            ,String siDo, String siGunGu, String eupMyeongDong);

    List<StoreDocument.Menu> findMenuByStoreId(UUID storeId);

    List<StoreDocument.Menu> findMenuByStoreIdAndMenuCategoryId(UUID storeId, UUID categoryId);

    Optional<StoreDocument.Menu> findMenuByMenuId(UUID menuId);

    List<StoreDocument.MenuOption> findMenuOptionByMenuIdOrderByAdditionalPrice(UUID menuId);

    Optional<StoreDocument.MenuOption> findMenuOptionByMenuOptionId(UUID menuOptionId);
}
