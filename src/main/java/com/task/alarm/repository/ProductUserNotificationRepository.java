package com.task.alarm.repository;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.ProductUserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductUserNotificationRepository extends JpaRepository<ProductUserNotification, Long> {

    @Query(value= """
                  select pun
                    from ProductUserNotification pun
              join fetch pun.product p
              join fetch pun.user u
                   where pun.product.id = :productId
                order by pun.id asc
                  """)
    List<ProductUserNotification> findAllByProductOrderByIdAsc(@Param("productId") Long productId);

    List<ProductUserNotification> findByProductAndIdLessThanOrderByIdAsc(Product product, Long lastAlarmUserId);
}
