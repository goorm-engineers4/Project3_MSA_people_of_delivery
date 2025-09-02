package com.example.cloudfour.storeservice.domain.collection.document;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStoreDocument_StoreCategory is a Querydsl query type for StoreCategory
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStoreDocument_StoreCategory extends BeanPath<StoreDocument.StoreCategory> {

    private static final long serialVersionUID = -899647498L;

    public static final QStoreDocument_StoreCategory storeCategory = new QStoreDocument_StoreCategory("storeCategory");

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final StringPath storeCategoryName = createString("storeCategoryName");

    public QStoreDocument_StoreCategory(String variable) {
        super(StoreDocument.StoreCategory.class, forVariable(variable));
    }

    public QStoreDocument_StoreCategory(Path<? extends StoreDocument.StoreCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStoreDocument_StoreCategory(PathMetadata metadata) {
        super(StoreDocument.StoreCategory.class, metadata);
    }

}

