package com.task.alarm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ProductUserNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int restockCount;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime sendAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
