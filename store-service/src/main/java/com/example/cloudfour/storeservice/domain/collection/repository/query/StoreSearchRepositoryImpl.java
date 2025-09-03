package com.example.cloudfour.storeservice.domain.collection.repository.query;

import com.example.cloudfour.storeservice.domain.collection.document.StoreDocument;
import com.example.cloudfour.storeservice.domain.menu.enums.MenuStatus;
import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.cloudfour.storeservice.domain.collection.document.QStoreDocument.storeDocument;

@Repository
public class StoreSearchRepositoryImpl extends QuerydslRepositorySupport implements StoreSearchRepository {

    private final MongoTemplate mongoTemplate;

    public StoreSearchRepositoryImpl(@Qualifier("mongoTemplate")MongoOperations operations, MongoTemplate mongoTemplate){
        super(operations);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<StoreDocument> findStoreByStoreId(UUID storeId) {
        StoreDocument store = from(storeDocument)
                .where(storeDocument.storeId.eq(storeId))
                .fetchOne();

        if (store == null) {
            return Optional.empty();
        }

        if (store.getMenus() != null) {
            List<StoreDocument.Menu> visibleMenus = store.getMenus().stream()
                    .filter(menu -> menu.getMenuStatus() != MenuStatus.숨김)
                    .collect(Collectors.toList());

            // 3. 필터링된 메뉴로 새로운 StoreDocument 생성
            StoreDocument filteredStore = StoreDocument.builder()
                    .id(store.getId())
                    .storeId(store.getStoreId())
                    .userId(store.getUserId())
                    .name(store.getName())
                    .address(store.getAddress())
                    .phone(store.getPhone())
                    .content(store.getContent())
                    .minPrice(store.getMinPrice())
                    .deliveryTip(store.getDeliveryTip())
                    .rating(store.getRating())
                    .likeCount(store.getLikeCount())
                    .reviewCount(store.getReviewCount())
                    .OperationHours(store.getOperationHours())
                    .closedDays(store.getClosedDays())
                    .siDo(store.getSiDo())
                    .siGunGu(store.getSiGunGu())
                    .eupMyeonDong(store.getEupMyeonDong())
                    .pictureURL(store.getPictureURL())
                    .createdAt(store.getCreatedAt())
                    .storeCategory(store.getStoreCategory())
                    .menus(visibleMenus)
                    .reviews(store.getReviews())
                    .build();

            return Optional.of(filteredStore);
        }
        return Optional.of(store);
    }

    @Override
    public Slice<StoreDocument> findAllStoreByCategoryAndCursor(UUID categoryId, LocalDateTime cursor, Pageable pageable, String siDo, String siGunGu, String eupMyeongDong) {
        BooleanBuilder regionBuilder = new BooleanBuilder();
        if(siDo!=null){
            regionBuilder.or(storeDocument.siDo.containsIgnoreCase(siDo));
        }
        if(siGunGu!=null){
            regionBuilder.or(storeDocument.siGunGu.containsIgnoreCase(siGunGu));
        }
        if(eupMyeongDong!=null){
            regionBuilder.or(storeDocument.eupMyeonDong.containsIgnoreCase(eupMyeongDong));
        }

        int pageSize = pageable.getPageSize();
        List<StoreDocument> stores = from(storeDocument)
                .where(storeDocument.storeCategory.id.eq(categoryId), regionBuilder
                        , storeDocument.createdAt.lt(cursor)).orderBy(storeDocument.createdAt.desc()).limit(pageSize+1)
                .fetch();

        boolean hasNext = stores.size() > pageSize;
        if(hasNext){
            stores.remove(pageSize);
        }
        return new SliceImpl<>(stores,pageable,hasNext);
    }

    @Override
    public Slice<StoreDocument> findAllStoreByKeyWordAndRegion(String keyword, LocalDateTime cursor, Pageable pageable, String siDo, String siGunGu, String eupMyeongDong) {
        int pageSize = pageable.getPageSize();

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(storeDocument.createdAt.lt(cursor));

        if(keyword!=null && !keyword.isEmpty()){
            builder.and(storeDocument.name.containsIgnoreCase(keyword)
                    .or(storeDocument.storeCategory.storeCategoryName.containsIgnoreCase(keyword)));
        }

        BooleanBuilder regionBuilder = new BooleanBuilder();
        if(siDo!=null){
            regionBuilder.or(storeDocument.siDo.containsIgnoreCase(siDo));
        }
        if(siGunGu!=null){
            regionBuilder.or(storeDocument.siGunGu.containsIgnoreCase(siGunGu));
        }
        if(eupMyeongDong!=null){
            regionBuilder.or(storeDocument.eupMyeonDong.containsIgnoreCase(eupMyeongDong));
        }
        builder.and(regionBuilder);

        List<StoreDocument> stores = from(storeDocument).where(builder)
                .orderBy(storeDocument.createdAt.desc()).limit(pageSize+1).fetch();
        boolean hasNext = stores.size() > pageSize;
        if(hasNext){
            stores.remove(pageSize);
        }
        return new SliceImpl<>(stores,pageable,hasNext);
    }

    @Override
    public List<StoreDocument.Menu> findMenuByStoreId(UUID storeId) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("storeId").is(storeId)
        );

        Query mongoQuery = new Query(criteria);
        mongoQuery.fields().include("menus");

        StoreDocument storeDocument = mongoTemplate.findOne(mongoQuery, StoreDocument.class);

        if (storeDocument == null || storeDocument.getMenus() == null) {
            return Collections.emptyList();
        }

        return storeDocument.getMenus().stream()
                .filter(menu -> menu.getMenuStatus() != MenuStatus.숨김)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoreDocument.Menu> findMenuByStoreIdAndMenuCategoryId(UUID storeId, UUID categoryId) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("storeId").is(storeId),
                Criteria.where("menus.menuCategoryId").is(categoryId)
        );

        Query mongoQuery = new Query(criteria);
        mongoQuery.fields().include("menus");

        StoreDocument result = mongoTemplate.findOne(mongoQuery,StoreDocument.class);

        if (result != null && result.getMenus() != null) {
            return result.getMenus().stream()
                    .filter(menu -> menu.getMenuCategory().getId().equals(categoryId))
                    .filter(menu -> menu.getMenuStatus() != MenuStatus.숨김)
                    .collect(Collectors.toList());
        }


        return Collections.emptyList();
    }

    @Override
    public Optional<StoreDocument.Menu> findMenuByMenuId(UUID menuId) {
        Criteria criteria = Criteria.where("menus.id").is(menuId)
                .and("menus.menuStatus").ne(MenuStatus.숨김.name());
        Query mongoQuery = new Query(criteria);
        mongoQuery.fields().include("menus.$");

        StoreDocument result = mongoTemplate.findOne(mongoQuery, StoreDocument.class);

        if(result != null && result.getMenus() != null && !result.getMenus().isEmpty()){
            return Optional.of(result.getMenus().getFirst());
        }

        return Optional.empty();
    }

    @Override
    public List<StoreDocument.MenuOption> findMenuOptionByMenuIdOrderByAdditionalPrice(UUID menuId) {
        Criteria criteria = Criteria.where("menus.id").is(menuId);
        Query mongoQuery = new Query(criteria);
        mongoQuery.fields().include("menus.$"); // 해당 메뉴만 포함

        StoreDocument result = mongoTemplate.findOne(mongoQuery, StoreDocument.class);

        if (result != null && result.getMenus() != null && !result.getMenus().isEmpty()) {
            StoreDocument.Menu menu = result.getMenus().getFirst();
            if (menu.getMenuOptions() != null) {
                return menu.getMenuOptions().stream()
                        .sorted(Comparator.comparing(StoreDocument.MenuOption::getAdditionalPrice))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Optional<StoreDocument.MenuOption> findMenuOptionByMenuOptionId(UUID menuOptionId) {
        Criteria criteria = Criteria.where("menus.menuOptions.id").is(menuOptionId);
        Query mongoQuery = new Query(criteria);
        mongoQuery.fields().include("menus"); // 전체 메뉴 배열 포함

        StoreDocument result = mongoTemplate.findOne(mongoQuery, StoreDocument.class);

        if (result != null && result.getMenus() != null) {
            return result.getMenus().stream()
                    .filter(menu -> menu.getMenuOptions() != null)
                    .flatMap(menu -> menu.getMenuOptions().stream())
                    .filter(option -> option.getId().equals(menuOptionId))
                    .findFirst();
        }

        return Optional.empty();
    }
}

