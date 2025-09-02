package com.example.cloudfour.storeservice.domain.collection.document;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStoreDocument_Review is a Querydsl query type for Review
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QStoreDocument_Review extends BeanPath<StoreDocument.Review> {

    private static final long serialVersionUID = -23265215L;

    public static final QStoreDocument_Review review = new QStoreDocument_Review("review");

    public final StringPath content = createString("content");

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final NumberPath<Float> score = createNumber("score", Float.class);

    public QStoreDocument_Review(String variable) {
        super(StoreDocument.Review.class, forVariable(variable));
    }

    public QStoreDocument_Review(Path<? extends StoreDocument.Review> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStoreDocument_Review(PathMetadata metadata) {
        super(StoreDocument.Review.class, metadata);
    }

}

