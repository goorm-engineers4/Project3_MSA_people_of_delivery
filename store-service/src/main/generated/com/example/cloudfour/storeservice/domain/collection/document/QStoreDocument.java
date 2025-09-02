package com.example.cloudfour.storeservice.domain.collection.document;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStoreDocument is a Querydsl query type for StoreDocument
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStoreDocument extends EntityPathBase<StoreDocument> {

    private static final long serialVersionUID = 644055365L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStoreDocument storeDocument = new QStoreDocument("storeDocument");

    public final StringPath address = createString("address");

    public final StringPath closedDays = createString("closedDays");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> deliveryTip = createNumber("deliveryTip", Integer.class);

    public final StringPath eupMyeonDong = createString("eupMyeonDong");

    public final StringPath id = createString("id");

    public final NumberPath<Integer> likeCount = createNumber("likeCount", Integer.class);

    public final ListPath<StoreDocument.Menu, QStoreDocument_Menu> menus = this.<StoreDocument.Menu, QStoreDocument_Menu>createList("menus", StoreDocument.Menu.class, QStoreDocument_Menu.class, PathInits.DIRECT2);

    public final NumberPath<Integer> minPrice = createNumber("minPrice", Integer.class);

    public final StringPath name = createString("name");

    public final StringPath OperationHours = createString("OperationHours");

    public final StringPath operationHours = createString("operationHours");

    public final StringPath phone = createString("phone");

    public final StringPath pictureURL = createString("pictureURL");

    public final NumberPath<Float> rating = createNumber("rating", Float.class);

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final ListPath<StoreDocument.Review, QStoreDocument_Review> reviews = this.<StoreDocument.Review, QStoreDocument_Review>createList("reviews", StoreDocument.Review.class, QStoreDocument_Review.class, PathInits.DIRECT2);

    public final StringPath siDo = createString("siDo");

    public final StringPath siGunGu = createString("siGunGu");

    public final QStoreDocument_StoreCategory storeCategory;

    public final ComparablePath<java.util.UUID> storeId = createComparable("storeId", java.util.UUID.class);

    public final ComparablePath<java.util.UUID> userId = createComparable("userId", java.util.UUID.class);

    public QStoreDocument(String variable) {
        this(StoreDocument.class, forVariable(variable), INITS);
    }

    public QStoreDocument(Path<? extends StoreDocument> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStoreDocument(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStoreDocument(PathMetadata metadata, PathInits inits) {
        this(StoreDocument.class, metadata, inits);
    }

    public QStoreDocument(Class<? extends StoreDocument> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.storeCategory = inits.isInitialized("storeCategory") ? new QStoreDocument_StoreCategory(forProperty("storeCategory")) : null;
    }

}

