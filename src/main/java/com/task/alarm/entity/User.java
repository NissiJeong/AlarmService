package com.task.alarm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String email;

    private String mobileNumber;

    public User(String mail, String number) {
        this.email = mail;
        this.mobileNumber = number;
    }
}
