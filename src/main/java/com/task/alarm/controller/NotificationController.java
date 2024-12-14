package com.task.alarm.controller;

import com.task.alarm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping(value = "/products/{productId}/notifications/re-stock")
    public ResponseEntity<?> restockAndNotification (@PathVariable Long productId) {
        return notificationService.restockAndNotification(productId);
    }
}
