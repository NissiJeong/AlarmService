package com.task.alarm.service;

import com.task.alarm.dto.StockChangeEvent;
import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductNotificationHistory;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.entity.RestockAlarmStatusEnum;
import com.task.alarm.repository.ProductNotificationHistoryRepository;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.ProductUserNotificationRepository;
import com.task.alarm.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ProductRepository productRepository;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationRepository productUserNotificationRepository;
    private final RedisRepository redisRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StockChangeEvenetListener stockChangeEvenetListener;

    // 상품 재입고 회차 1 증가 했다는 것은 10개의 재고가 증가한다고 가정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> restockAndNotification(Long productId) {

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
        //sendAlarm(product);
        stockChangeEvenetListener.handleStockChangeEvent(new StockChangeEvent(product.getId(), product.getStockCount()));

        return ResponseEntity.ok(product);
    }

    private void sendAlarm(Product product) {
        // 재입고 알림 설정 유저 select
        List<ProductUserNotification> alarmUsers = productUserNotificationRepository.findAllByProductOrderByIdAsc(product.getId());

        // 알림 전송 시점에 재고 수량 MySQL 에서 가져와서 Redis 에 저장
        redisRepository.saveProductStockCount(product);

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
    }

    // admin 호출 되는 순간 마지막으로 알림 전송한 사람 이후로 알림 전송 시작해야 함.
    // product 의 id, restock count 로 상태, 마지막 알림 전송된 사용자 가져와서 상태값이 에러로 시작된 경우 재 전송 시작
    public ResponseEntity<?> adminRestockAndNotification(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()->
            new NullPointerException("해당 상품이 존재하지 않습니다.")
        );
        // 상품별, 회차별 재입고 알림 발송상태, 마지막 발송 유저 아이디 가져오기
        ProductNotificationHistory productNotificationHistory = productNotificationHistoryRepository.findByProductIdAndRestockCount(productId, product.getRestockCount());

        RestockAlarmStatusEnum alaramStatus = productNotificationHistory.getRestockAlarmStatus();
        Long lastAlarmUserId = productNotificationHistory.getLastAlarmUserId();
        // sold out or error 인 경우 마지막 발송 유저 이후로 다시 전송
        if(alaramStatus.equals(RestockAlarmStatusEnum.ERROR) || alaramStatus.equals(RestockAlarmStatusEnum.SOLD_OUT)) {
            // 재입고 알림 설정 유저 select
            List<ProductUserNotification> alarmUsers = productUserNotificationRepository.findByProductAndIdLessThanOrderByIdAsc(product, lastAlarmUserId);

            for (ProductUserNotification alarmUser : alarmUsers) {
                System.out.println("alarmUser.getUser().getId() = " + alarmUser.getUser().getId());
            }
        }

        return null;
    }

    // 임시로 sold out 이벤트 발생시켜서 알림 보내는 로직에 이벤트 전송
    // 실제라면 구매가 이루어질 떄마다 재고가 decrease 되며 재고가 0 이 되는 경우 알림을 중단하는 기능 구현 가능.
    @Transactional
    public void productStockSoldOut(Long productId) {
        //Redis 재고 카운트 0 으로 변경, MySQL 재고 카운트 0으로 변경
        Product product = productRepository.findById(productId).orElseThrow(()->
                new NullPointerException("해당 상품이 존재하지 않습니다.")
        );

        // MySQL0, Redis 0 으로 변경
        product.updateStockCount(0);
        redisRepository.saveProductStockCount(product);

        eventPublisher.publishEvent(new StockChangeEvent(productId, 0));
    }
}
