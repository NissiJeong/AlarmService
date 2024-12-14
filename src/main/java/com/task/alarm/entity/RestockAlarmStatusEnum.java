package com.task.alarm.entity;

public enum RestockAlarmStatusEnum {
    PROGRESS(Status.PROGRESS),  // 사용자 권한
    SOLD_OUT(Status.SOLD_OUT),
    ERROR(Status.ERROR),
    COMPLETED(Status.COMPLETED);

    private final String status;

    RestockAlarmStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public static class Status {
        public static final String PROGRESS = "IN_PROGRESS";
        public static final String SOLD_OUT = "CANCELED_BY_SOLD_OUT";
        public static final String ERROR = "CANCELED_BY_ERROR";
        public static final String COMPLETED = "COMPLETED";
    }
}
