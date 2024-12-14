package com.task.alarm.entity;

import jakarta.persistence.*;

@Entity
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
}
