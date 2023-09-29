package com.example.telegram_bot.repository;

import com.example.telegram_bot.model.Ads;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdsRepository extends JpaRepository<Ads, Long> {
}
