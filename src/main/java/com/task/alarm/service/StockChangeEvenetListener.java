package com.task.alarm.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import com.task.alarm.dto.StockChangeEvent;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.entity.RestockAlarmStatusEnum;
import com.task.alarm.repository.ProductUserNotificationRepository;
import com.task.alarm.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StockChangeEvenetListener {

    private final ProductUserNotificationRepository productUserNotificationRepository;
    private final RedisRepository redisRepository;

    @EventListener
    public void handleStockChangeEvent(StockChangeEvent event) {
        // 재입고 알림 설정 유저 select
        List<ProductUserNotification> alarmUsers = productUserNotificationRepository.findAllByProductOrderByIdAsc(event.getProductId());
        // 알림 발송 후 한 번에 저장하기 위한 배열 변수
        List<String> userIds = new ArrayList<>();

        int checkIndex = 0;
        for(ProductUserNotification productUserNoti : alarmUsers) {
            // 재고가 0일 경우 알림을 중단하는 로직
            if (event.getNewQuantity() == 0) {
                // 알림 중단 로직 추가
                // 품절에 의한 알림 발송 중단 상태 저장
                redisRepository.saveProductRestockStatus(productUserNoti.getProduct(), RestockAlarmStatusEnum.SOLD_OUT.getStatus());

                // 마지막으로 알림 보낸 사용자 저장
                redisRepository.saveLastNotificationUser(productUserNoti.getProduct(), productUserNoti.getUser());

                break;
            }

            sendAlarm(productUserNoti, checkIndex, userIds, alarmUsers.size()-1);

            checkIndex++;
        }

        // 알람 사용자가 있고 보낸 사용자가 있으면 Redis 에 값 저장
        if(!alarmUsers.isEmpty() && !userIds.isEmpty()){
            redisRepository.saveProductUserNotificationInfoList(alarmUsers.get(0).getProduct(), userIds);
        }
    }

    // 1초에 500번만 호출을 허용
    @RateLimiter(name = "sendNotificationLimiter", fallbackMethod = "sendAlarmFallback")
    public void sendAlarm(ProductUserNotification productUserNoti, int checkIndex, List<String> userIds, int lastIdx) {
        System.out.println("======================"+checkIndex+"=============================");


        // Redis 에 저장할 값 한번에 저장하기 위한 로직
        userIds.add(String.valueOf(productUserNoti.getUser().getId()));

        // 마지막 사용자인 경우
        if(checkIndex == lastIdx) {
            // 알림 완료 상태 저장
            redisRepository.saveProductRestockStatus(productUserNoti.getProduct(), RestockAlarmStatusEnum.COMPLETED.getStatus());

            // 마지막으로 알림 보낸 사용자 저장
            redisRepository.saveLastNotificationUser(productUserNoti.getProduct(), productUserNoti.getUser());
        }
    }

    // Rate limit을 초과했을 때 호출되는 fallback 메서드
    public void sendAlarmFallback(ProductUserNotification productUserNoti, int checkIndex, List<String> userIds, int lastIdx, Throwable throwable) {
        int retryCount = 0;
        long backoffTime = 1000; // 초기 대기 시간 1초

        while (retryCount < 3) {
            retryCount++;
            try {
                Thread.sleep(backoffTime); // 지수 백오프 적용
                sendAlarm(productUserNoti, checkIndex, userIds, lastIdx); // 원래 로직 재실행
                return; // 성공하면 메서드 종료
            } catch (Exception e) {
                System.out.println("Retry attempt " + retryCount + " failed.");
                backoffTime *= 2; // 대기 시간을 두 배로 늘림
            }
        }
        System.out.println("All retry attempts failed for user: " + productUserNoti.getUser().getId());
    }
}
