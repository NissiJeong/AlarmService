package com.task.alarm.service;

import com.task.alarm.entity.Product;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisRepository redisRepository;
    private final RedissonClient redissonClient;

    // 스케쥴 돌면서 1초에 한번씩 재고 수량 -1
    // Redis 에 있는 수량 decrease 후 모든 상품 처리하면 MySQL 과 동기화
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void decreaseProductStock() {
        String lockKey = "lock:sync:product";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                // Redis에서 동기화할 키 조회
                Set<String> keys = redisRepository.getProductStockKey();
                if (keys == null || keys.isEmpty()) return;

                for (String key : keys) {
                    Long productId = Long.valueOf(key.split(":")[1]);
                    redisRepository.decreaseProductStock(productId);
                    syncProductStockData(productId);
                }
            } else {
                System.out.println("동기화 작업 중복 실행 방지됨");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("동기화 중 락 획득 실패", e);
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected void syncProductStockData(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
        );
        int stockCount = redisRepository.findProductStockCount(product);
        product.updateStockCount(stockCount);
    }
}
