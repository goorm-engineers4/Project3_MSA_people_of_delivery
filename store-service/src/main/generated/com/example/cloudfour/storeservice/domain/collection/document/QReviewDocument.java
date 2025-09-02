package com.example.cloudfour.storeservice.domain.collection.document;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReviewDocument is a Querydsl query type for ReviewDocument
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewDocument extends EntityPathBase<ReviewDocument> {

    private static final long serialVersionUID = 933893514L;

    public static final QReviewDocument reviewDocument = new QReviewDocument("reviewDocument");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath id = createString("id");

    public final StringPath pictureUrl = createString("pictureUrl");

    public final ComparablePath<java.util.UUID> reviewId = createComparable("reviewId", java.util.UUID.class);

    public final NumberPath<Float> score = createNumber("score", Float.class);

    public final ComparablePath<java.util.UUID> storeId = createComparable("storeId", java.util.UUID.class);

    public final ComparablePath<java.util.UUID> userId = createComparable("userId", java.util.UUID.class);

    public final StringPath userName = createString("userName");

    public QReviewDocument(String variable) {
        super(ReviewDocument.class, forVariable(variable));
    }

    public QReviewDocument(Path<? extends ReviewDocument> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewDocument(PathMetadata metadata) {
        super(ReviewDocument.class, metadata);
    }

}

