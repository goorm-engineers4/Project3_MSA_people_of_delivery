package com.example.cloudfour.paymentservice.domain.payment.apiclient;

import com.example.cloudfour.paymentservice.commondto.StoreResponseDTO;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentErrorCode;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreClient {
    
    private final StoreFeignClient storeClient;

    public boolean existsStore(UUID storeId) {
        try {
            Boolean exists = storeClient.existsStore(storeId);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("가게 존재 여부 확인 실패: storeId={}", storeId, e);
            throw new PaymentException(PaymentErrorCode.STORE_VALIDATION_FAILED);
        }
    }

    public StoreResponseDTO getStoreById(UUID storeId) {
        try {
            StoreResponseDTO store = storeClient.getStoreById(storeId);
            if (store == null) {
                throw new PaymentException(PaymentErrorCode.STORE_NOT_FOUND);
            }
            return store;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("가게 정보 조회 실패: storeId={}", storeId, e);
            throw new PaymentException(PaymentErrorCode.STORE_VALIDATION_FAILED);
        }
    }
}

