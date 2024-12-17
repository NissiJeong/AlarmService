package com.task.alarm.service;

import com.task.alarm.dto.StockChangeEvent;
import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.entity.RestockAlarmStatusEnum;
import com.task.alarm.repository.ProductNotificationHistoryRepository;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.ProductUserNotificationRepository;
import com.task.alarm.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
            } else {
                // Redis 에 저장할 값 한번에 저장하기 위한 로직
                userIds.add(String.valueOf(productUserNoti.getUser().getId()));

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

        // 알람 사용자가 있고 보낸 사용자가 있으면 Redis 에 값 저장
        if(!alarmUsers.isEmpty() && !userIds.isEmpty()){
            System.out.println("alarmUsers.get(0).getProduct().getId() = " + alarmUsers.get(0).getProduct().getId());
            userIds.forEach(System.out::println);
            redisRepository.saveProductUserNotificationInfoList(alarmUsers.get(0).getProduct(), userIds);
        }
    }
}
