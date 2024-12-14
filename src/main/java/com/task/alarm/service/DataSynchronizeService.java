package com.task.alarm.service;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductNotificationHistory;
import com.task.alarm.entity.ProductUserNotificationHistory;
import com.task.alarm.repository.*;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DataSynchronizeService {

    private final ProductRepository productRepository;
    private final RedisRepository redisRepository;
    private final RedissonClient redissonClient;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationHistoryRepository productUserNotificationHistoryRepository;
    private final UserRepository userRepository;

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
                Set<String> keys = redisRepository.getKeySet("product:*:stock");
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

    public void syncProductStockData(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
        );
        int stockCount = redisRepository.findProductStockCount(product);
        product.updateStockCount(stockCount);
    }

    // 알림 종료후 Redis 와 MySQL 데이터 동기화
    // 1. 마지막 알림 사용자
    // 2. 상품 재입고 알림 사용자 내역
    // 3. 알림 전송 상태
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void synchronizeRedisMysqlData() {
        String lockKey = "lock:sync:product:restock:lastAlarmUser";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도
            if (lock.tryLock(10, 10, TimeUnit.SECONDS)) {
                // Redis에서 동기화할 키 조회
                Set<String> keys = redisRepository.getKeySet("product:*:restockcnt:*:lastNotification");
                if (keys == null || keys.isEmpty()) return;

                for (String key : keys) {
                    String[] keyArr = key.split(":");
                    Long productId = Long.valueOf(keyArr[1]);
                    Integer restockCount = Integer.valueOf(keyArr[3]);

                    syncProductNotificationHistory(productId, restockCount);
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

    private void syncProductNotificationHistory(Long productId, Integer restockCount) {
        ProductNotificationHistory productNotificationHistory = productNotificationHistoryRepository.findByProductIdAndRestockCount(productId, restockCount);
        if(productNotificationHistory == null) {
            throw new NullPointerException("해당 상품 재입고 히스토리가 없습니다.");
        }
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
        );

        // Redis 에서 lastUserId, status 가져와서 MySQL 에 저장
        String key = "product:"+productId+":restockcnt:"+restockCount+":status";
        String status = String.valueOf(redisRepository.getValue(key));

        key = "product:"+productId+":restockcnt:"+restockCount+":lastNotification";
        Long lastAlarmUserId = Long.parseLong(String.valueOf(redisRepository.getValue(key)));

        productNotificationHistory.updateRestockAlarmStatusAndLastAlarmUserId(status, lastAlarmUserId);

        // alarm, restockCnt 로 알람 보낸 user 데이터 가져와서 내역에 저장
        key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":alarm";
        List<String> userAlarmList = redisRepository.getEntireList(key);
        List<ProductUserNotificationHistory> productUserNotificationHistoryList =
                userAlarmList
                        .stream()
                        .map(
                                userId ->
                                    new ProductUserNotificationHistory(userRepository.findById(Long.valueOf(userId)).orElseThrow(),
                                                                        restockCount,
                                                                null,
                                                                        product))
                        .toList();

        productUserNotificationHistoryRepository.saveAll(productUserNotificationHistoryList);

        // 동기화 완료 후 Redis 데이터 삭제
        key = "product:"+productId+":restockcnt:"+restockCount+":status";
        redisRepository.deleteDataByKey(key);
        key = "product:"+productId+":restockcnt:"+restockCount+":lastNotification";
        redisRepository.deleteDataByKey(key);
        key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":alarm";
        redisRepository.deleteDataByKey(key);
    }
}
