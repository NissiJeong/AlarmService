package com.task.alarm.service;

import com.task.alarm.entity.Product;
import com.task.alarm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProductRepository productRepository;

    private Product product;
}
