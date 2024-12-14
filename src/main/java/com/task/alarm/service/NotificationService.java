package com.task.alarm.service;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.ProductUserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ProductRepository productRepository;

    private final ProductUserNotificationRepository productUserNotificationRepository;

    // 상품 재입고 회차 1 증가 했다는 것은 10개의 재고가 증가한다고 가정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> restockAndNotification(Long productId) {
        // 상품이 없으면 알림 발송 못 함
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
        );

        // 재입고 회차 1 증가, 10개 재고 증가
        product.updateRestockCountAndStockCount(1, 10);

        // 알림 보내기
        sendAlarm(product);

        return ResponseEntity.ok(product);
    }

    private void sendAlarm(Product product) {
        // 재입고 알림 설정 유저 select
        List<ProductUserNotification> alarmUsers = productUserNotificationRepository.findAllByProductOrderByIdAsc(product.getId());

        for (ProductUserNotification alarmUser : alarmUsers) {
            System.out.println("alarmUser.getUser().getEmail() = " + alarmUser.getUser().getEmail());
            System.out.println("alarmUser.getProduct().getId() = " + alarmUser.getProduct().getId());
        }
        // 알림 설정 유저 알림 send

        //
    }
}
