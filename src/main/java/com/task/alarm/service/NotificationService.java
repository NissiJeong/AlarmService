package com.task.alarm.service;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductNotificationHistory;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.entity.RestockAlarmStatusEnum;
import com.task.alarm.repository.ProductNotificationHistoryRepository;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.ProductUserNotificationRepository;
import com.task.alarm.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationRepository productUserNotificationRepository;
    private final RedisRepository redisRepository;
    private final RedissonClient redissonClient;

    // 상품 재입고 회차 1 증가 했다는 것은 10개의 재고가 증가한다고 가정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> restockAndNotification(Long productId) {
        String lockKey = "lock:product:alarm:"+productId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean available = false;

        try {
            available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if(!available) {
                throw new IllegalArgumentException("Lock 획득 실패");
            }

            // 상품이 없으면 알림 발송 못 함
            Product product = productRepository.findById(productId).orElseThrow(() ->
                    new NullPointerException("해당 상품을 찾을 수 없습니다.")
            );

            // 재입고 회차 1 증가, 10개 재고 증가
            product.updateRestockCountAndStockCount(1, 10);
            // ProductNotificationHistory (상품별 재입고 알림 히스토리) 에 데이터 저장
            ProductNotificationHistory productNotificationHistory = new ProductNotificationHistory(product);
            productNotificationHistoryRepository.save(productNotificationHistory);

            // 알림 보내기
            sendAlarm(product);

            return ResponseEntity.ok(product);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if(available)
                lock.unlock();
        }
    }

    private void sendAlarm(Product product) {
        // 재입고 알림 설정 유저 select
        List<ProductUserNotification> alarmUsers = productUserNotificationRepository.findAllByProductOrderByIdAsc(product.getId());

        // 알림 전송 시점에 재고 수량 MySQL 에서 가져와서 Redis 에 저장
        redisRepository.saveProductStockCount(product);

        // 한 명 씩 알림 보내면서 재고 체크 하고 없으면 알림 중지
        // 알림 메시지 1초에 최대 500 개의 요청: MySQL 에 직접 저장하면 비효율적
        // Redis 에 productId, userId 키로 잡아서 저장
        // 추후 Redis 와 MySQL 동기화
        int checkIndex = 0;
        for(ProductUserNotification productUserNoti : alarmUsers) {
            // 재고 수량 체크
            int stockCount = redisRepository.findProductStockCount(product);
            // 재고 수량이 0이면 더이상 알림 보내지 않음.
            if(stockCount == 0) {
                // 품절에 의한 알림 발송 중단 상태 저장
                redisRepository.saveProductRestockStatus(productUserNoti.getProduct(), RestockAlarmStatusEnum.SOLD_OUT.getStatus());

                // 마지막으로 알림 보낸 사용자 저장
                redisRepository.saveLastNotificationUser(productUserNoti.getProduct(), productUserNoti.getUser());
                break;
            }
            // 알림 설정 유저에게 알림 send

            // 알림 내용 저장 Redis 에 productId, userId 키로 잡아서 저장
            redisRepository.saveProductUserNotificationInfo(productUserNoti.getProduct(), productUserNoti.getUser());
            // 마지막 사용자인 경우
            if(checkIndex == alarmUsers.size()-1) {
                // 알림 완료 상태 저장
                redisRepository.saveProductRestockStatus(productUserNoti.getProduct(), RestockAlarmStatusEnum.COMPLETED.getStatus());

                // 마지막으로 알림 보낸 사용자 저장
                redisRepository.saveLastNotificationUser(productUserNoti.getProduct(), productUserNoti.getUser());
            }
            checkIndex++;
        }

        // 알림 종료후 Redis 와 MySQL 데이터 동기화
        // 1. 마지막 알림 사용자
        // 2. 상품 재입고 알림 사용자 내역
        // 3. 알림 전송 상태
    }
}
