package com.task.alarm.config;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductUserNotification;
import com.task.alarm.entity.User;
import com.task.alarm.repository.ProductRepository;
import com.task.alarm.repository.ProductUserNotificationRepository;
import com.task.alarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductUserNotificationRepository productUserNotificationRepository;

    @Override
    public void run(String... args) throws Exception {
        if(productRepository.count() == 0 &&
                userRepository.count() == 0 &&
                productUserNotificationRepository.count() == 0
        ) {
            Product product = productRepository.save(new Product(0,0));
            for(int i=0; i<500; i++) {
                User user = userRepository.save(new User("banisii@naver.com", "01034118840"));
                ProductUserNotification productUserNotification = productUserNotificationRepository.save(new ProductUserNotification(false, product, user));
            }
        }
    }
}
