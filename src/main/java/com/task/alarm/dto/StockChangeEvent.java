package com.task.alarm.dto;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class StockChangeEvent {
    private final Long productId;
    private final int newQuantity;
}
