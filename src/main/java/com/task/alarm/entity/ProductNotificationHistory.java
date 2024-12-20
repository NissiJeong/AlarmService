package com.task.alarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Getter
public class ProductNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int restockCount;

    @Enumerated(value = EnumType.STRING)
    private RestockAlarmStatusEnum restockAlarmStatus;

    private Long lastAlarmUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductNotificationHistory(Product product) {
        this.product = product;
        this.restockCount = product.getRestockCount();
    }

    public void updateRestockAlarmStatusAndLastAlarmUserId(String status, Long lastAlarmUserId) {
        this.restockAlarmStatus = RestockAlarmStatusEnum.valueOf(status);
        this.lastAlarmUserId = lastAlarmUserId;
    }
}
