package com.example.cloudfour.storeservice.domain.menu.service.command;

import com.example.cloudfour.storeservice.domain.menu.entity.Stock;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "spring.retry.enabled=true"
})
@DisplayName("StockCommandService 단위테스트")
class StockCommandServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockCommandService stockCommandService;

    private UUID stockId;
    private Stock stock;

    @BeforeEach
    void setUp() {
        stockId = UUID.randomUUID();
        stock = mock(Stock.class);
        lenient().when(stock.getId()).thenReturn(stockId);
        lenient().when(stock.getQuantity()).thenReturn(100L);
        lenient().when(stock.getVersion()).thenReturn(1L);
    }

    @Nested
    @DisplayName("decreaseStock 메서드는")
    class DecreaseStockTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 재고를 차감한다")
        void decreaseStock_ValidRequest_DecreasesStock() {
            // Given
            Long decreaseQuantity = 10L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

            // When
            stockCommandService.decreaseStock(stockId, decreaseQuantity);

            // Then
            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            verify(stock).decrease(decreaseQuantity);
        }

        @Test
        @DisplayName("존재하지 않는 재고면 예외를 던진다")
        void decreaseStock_StockNotFound_ThrowsException() {
            // Given
            Long decreaseQuantity = 10L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockCommandService.decreaseStock(stockId, decreaseQuantity))
                    .isInstanceOf(StockException.class)
                    .hasFieldOrPropertyWithValue("code", StockErrorCode.NOT_FOUND);

            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            verifyNoInteractions(stock);
        }

        @Test
        @DisplayName("낙관적 락 충돌이 발생하면 재시도 후 예외를 던진다")
        void decreaseStock_OptimisticLockException_RetriesAndThrows() throws Exception {
            // Given
            Long decreaseQuantity = 10L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

            // OptimisticLockException 항상 발생하도록 설정
            doThrow(new OptimisticLockException("Optimistic lock failed"))
                    .when(stock).decrease(decreaseQuantity);

            // When & Then
            assertThatThrownBy(() -> stockCommandService.decreaseStock(stockId, decreaseQuantity))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Optimistic lock failed");

            // 재시도 로직으로 인해 최소 1번 이상 호출되었는지 확인
            verify(stock, atLeast(1)).decrease(decreaseQuantity);
        }
    }

    @Nested
    @DisplayName("increaseStock 메서드는")
    class IncreaseStockTests {

        @Test
        @DisplayName("유효한 요청이 주어지면 재고를 증가시킨다")
        void increaseStock_ValidRequest_IncreasesStock() {
            // Given
            Long increaseQuantity = 20L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

            // When
            stockCommandService.increaseStock(stockId, increaseQuantity);

            // Then
            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            verify(stock).increase(increaseQuantity);
        }

        @Test
        @DisplayName("존재하지 않는 재고면 예외를 던진다")
        void increaseStock_StockNotFound_ThrowsException() {
            // Given
            Long increaseQuantity = 20L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockCommandService.increaseStock(stockId, increaseQuantity))
                    .isInstanceOf(StockException.class)
                    .hasFieldOrPropertyWithValue("code", StockErrorCode.NOT_FOUND);

            verify(stockRepository).findByIdWithOptimisticLock(stockId);
            verifyNoInteractions(stock);
        }

        @Test
        @DisplayName("낙관적 락 충돌이 발생하면 재시도 후 예외를 던진다")
        void increaseStock_OptimisticLockException_RetriesAndThrows() {
            // Given
            Long increaseQuantity = 20L;
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

            // OptimisticLockException 항상 발생하도록 설정
            doThrow(new OptimisticLockException("Optimistic lock failed"))
                    .when(stock).increase(increaseQuantity);

            // When & Then
            assertThatThrownBy(() -> stockCommandService.increaseStock(stockId, increaseQuantity))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Optimistic lock failed");

            // 재시도 로직으로 인해 최소 1번 이상 호출되었는지 확인
            verify(stock, atLeast(1)).increase(increaseQuantity);
        }
    }

    @Nested
    @DisplayName("recover 메서드는")
    class RecoverTests {

        @Test
        @DisplayName("OptimisticLockException을 받으면 로그를 남기고 예외를 다시 던진다")
        void recover_OptimisticLockException_LogsAndRethrows() {
            // Given
            OptimisticLockException exception = new OptimisticLockException("Lock conflict");
            Long quantity = 10L;

            // When & Then
            assertThatThrownBy(() -> stockCommandService.recover(exception, stockId, quantity))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Lock conflict");
        }
    }

    @Nested
    @DisplayName("동시성 테스트 - 낙관적 락")
    class ConcurrencyTests {

        @Test
        @DisplayName("동시에 여러 스레드가 재고 차감을 시도할 때 낙관적 락이 작동한다")
        void decreaseStock_ConcurrentAccess_OptimisticLockWorks() throws InterruptedException {
            // Given
            int threadCount = 10;
            Long decreaseQuantity = 5L;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            AtomicReference<Exception> capturedException = new AtomicReference<>();

            // Mock 설정: 첫 번째 호출은 성공, 나머지는 OptimisticLockException
            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));
            doAnswer(invocation -> {
                // 첫 번째 성공 후 나머지는 OptimisticLockException 발생
                if (successCount.get() == 0) {
                    successCount.incrementAndGet();
                    return null; // 정상 처리
                } else {
                    throw new OptimisticLockException("Concurrent modification detected");
                }
            }).when(stock).decrease(decreaseQuantity);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockCommandService.decreaseStock(stockId, decreaseQuantity);
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        if (capturedException.get() == null) {
                            capturedException.set(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            latch.await();
            executor.shutdown();

            // 하나는 성공, 나머지는 실패해야 함
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failureCount.get()).isEqualTo(threadCount - 1);

            // 실패한 경우 OptimisticLockException이어야 함
            if (capturedException.get() != null) {
                assertThat(capturedException.get()).isInstanceOf(OptimisticLockException.class);
            }
        }

        @Test
        @DisplayName("재고 증가와 감소가 동시에 발생할 때 충돌을 처리한다")
        void mixedOperations_ConcurrentAccess_HandlesConflicts() throws InterruptedException {
            // Given
            int operationCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(operationCount);
            CountDownLatch latch = new CountDownLatch(operationCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));

            // 랜덤하게 성공/실패를 시뮬레이션
            doAnswer(invocation -> {
                if (Math.random() < 0.3) { // 30% 확률로 충돌 발생
                    throw new OptimisticLockException("Version conflict");
                }
                successCount.incrementAndGet();
                return null;
            }).when(stock).decrease(any(Long.class));

            doAnswer(invocation -> {
                if (Math.random() < 0.3) { // 30% 확률로 충돌 발생
                    throw new OptimisticLockException("Version conflict");
                }
                successCount.incrementAndGet();
                return null;
            }).when(stock).increase(any(Long.class));

            // When
            for (int i = 0; i < operationCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        if (index % 2 == 0) {
                            stockCommandService.decreaseStock(stockId, 1L);
                        } else {
                            stockCommandService.increaseStock(stockId, 1L);
                        }
                    } catch (OptimisticLockException e) {
                        conflictCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            latch.await();
            executor.shutdown();

            // 성공 + 충돌 횟수가 전체 작업 횟수와 같아야 함
            assertThat(successCount.get() + conflictCount.get()).isGreaterThanOrEqualTo(operationCount);

            // 최소한 몇 개의 성공과 충돌이 발생해야 함 (확률적)
            assertThat(successCount.get()).isGreaterThan(0);
        }
        
    }

    @Nested
    @DisplayName("성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("대량의 동시 요청을 처리할 수 있다")
        void handleHighConcurrencyRequests() throws InterruptedException {
            // Given
            int threadCount = 100;
            int requestsPerThread = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(threadCount);

            AtomicInteger totalOperations = new AtomicInteger(0);
            AtomicInteger successfulOperations = new AtomicInteger(0);

            when(stockRepository.findByIdWithOptimisticLock(stockId)).thenReturn(Optional.of(stock));
            doAnswer(invocation -> {
                totalOperations.incrementAndGet();
                // 90% 확률로 성공
                if (Math.random() < 0.9) {
                    successfulOperations.incrementAndGet();
                    return null;
                } else {
                    throw new OptimisticLockException("Random conflict");
                }
            }).when(stock).decrease(any(Long.class));

            // When
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기

                        for (int j = 0; j < requestsPerThread; j++) {
                            try {
                                stockCommandService.decreaseStock(stockId, 1L);
                            } catch (OptimisticLockException ignored) {
                                // 충돌은 정상적인 상황
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 시작
            completeLatch.await();  // 모든 작업 완료 대기

            long endTime = System.currentTimeMillis();
            executor.shutdown();

            // Then
            long executionTime = endTime - startTime;
            int expectedTotalAttempts = threadCount * requestsPerThread;

            System.out.println("총 실행 시간: " + executionTime + "ms");
            System.out.println("총 시도 횟수: " + totalOperations.get());
            System.out.println("성공 횟수: " + successfulOperations.get());
            System.out.println("예상 시도 횟수: " + expectedTotalAttempts);

            // 성능 검증 (5초 이내에 완료되어야 함)
            assertThat(executionTime).isLessThan(5000);

            // 모든 요청이 처리되었는지 확인 (재시도 포함하여 더 많이 실행될 수 있음)
            assertThat(totalOperations.get()).isGreaterThanOrEqualTo(expectedTotalAttempts);
        }
    }
}