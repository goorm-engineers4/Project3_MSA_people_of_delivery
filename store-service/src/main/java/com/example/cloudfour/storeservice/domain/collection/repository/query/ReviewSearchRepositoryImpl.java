package com.example.cloudfour.storeservice.domain.collection.repository.query;

import com.example.cloudfour.storeservice.domain.collection.document.ReviewDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.cloudfour.storeservice.domain.collection.document.QReviewDocument.reviewDocument;


@Repository
public class ReviewSearchRepositoryImpl extends QuerydslRepositorySupport implements ReviewSearchRepository {

    public ReviewSearchRepositoryImpl(@Qualifier("mongoTemplate") MongoOperations operations){
        super(operations);
    }

    @Override
    public Slice<ReviewDocument> findAllByUserId(UUID userId, LocalDateTime cursor, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<ReviewDocument> stores = from(reviewDocument)
                .where(reviewDocument.userId.eq(userId)
                        , reviewDocument.createdAt.lt(cursor)).orderBy(reviewDocument.createdAt.desc()).limit(pageSize+1)
                .fetch();

        boolean hasNext = stores.size() > pageSize;
        if(hasNext){
            stores.remove(pageSize);
        }
        return new SliceImpl<>(stores,pageable,hasNext);
    }

    @Override
    public Slice<ReviewDocument> findAllByStoreId(UUID storeId, LocalDateTime cursor, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        List<ReviewDocument> stores = from(reviewDocument)
                .where(reviewDocument.storeId.eq(storeId)
                        , reviewDocument.createdAt.lt(cursor)).orderBy(reviewDocument.createdAt.desc()).limit(pageSize+1)
                .fetch();

        boolean hasNext = stores.size() > pageSize;
        if(hasNext){
            stores.remove(pageSize);
        }
        return new SliceImpl<>(stores,pageable,hasNext);
    }

    @Override
    public Optional<ReviewDocument> findById(UUID reviewId) {
        return Optional.ofNullable(from(reviewDocument).
                where(reviewDocument.reviewId.eq(reviewId)).fetchFirst());
    }
}