package com.example.telegram_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long chatId;
    @Column(unique = true, nullable = false)
    private String userName;
    private String firstName;
    private String lastName;
    private Timestamp registeredAt;
}
