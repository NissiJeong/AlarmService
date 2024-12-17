package com.task.alarm.repository;

import com.task.alarm.entity.Product;
import com.task.alarm.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public void saveProductUserNotificationInfo(Product product, User user) {
        String key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":alarm";
        ListOperations<String, String> listOps = redisTemplate.opsForList();//.set(key, String.valueOf(user.getId()));
        listOps.rightPush(key, String.valueOf(user.getId()));
    }

    public void saveProductUserNotificationInfoList(Product product, List<String> userIds) {
        String key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":alarm";
        ListOperations<String, String> listOps = redisTemplate.opsForList();//.set(key, String.valueOf(user.getId()));
        listOps.rightPushAll(key, userIds);
    }

    public void saveProductStockCount(Product product) {
        String key = "product:"+product.getId()+":stock";
        int value = product.getStockCount();

        redisTemplate.opsForValue().set(key, String.valueOf(value));
    }

    public int findProductStockCount(Product product) {
        String key = "product:"+product.getId()+":stock";
        return Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(key)));
    }

    public void saveLastNotificationUser(Product product, User user) {
        String key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":lastNotification";
        redisTemplate.opsForValue().set(key, String.valueOf(user.getId()));
    }

    public void saveProductRestockStatus(Product product, String status) {
        String key = "product:"+product.getId()+":restockcnt:"+product.getRestockCount()+":status";
        redisTemplate.opsForValue().set(key, status);
    }

    public Set<String> getKeySet(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public void decreaseProductStock(Long productId) {
        String key = "product:"+productId+":stock";

        if(Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(key))) >= 10)
            redisTemplate.opsForValue().decrement(key, 10);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 전체 리스트 가져오기
    public List<String> getEntireList(String key) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        return listOps.range(key, 0, -1); // 전체 리스트 가져오기
    }

    public void deleteDataByKey(String key) {
        redisTemplate.delete(key);
    }
}
