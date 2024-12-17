package com.task.alarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private int restockCount;

    private int stockCount;

    public Product(int restockCount, int stockCount) {
        this.restockCount = restockCount;
        this.stockCount = stockCount;
    }

    public void updateRestockCountAndStockCount(int restockCount, int stockCount) {
        this.restockCount += restockCount;
        this.stockCount += stockCount;
    }

    public void updateStockCount(int stockCount) {
        this.stockCount = stockCount;
    }
}
