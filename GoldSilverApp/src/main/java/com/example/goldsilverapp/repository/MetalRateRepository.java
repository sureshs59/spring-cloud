package com.example.goldsilverapp.repository;

import com.example.goldsilverapp.entity.MetalRateEntity;
import com.example.goldsilverapp.model.MetalRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MetalRateRepository extends JpaRepository<MetalRateEntity, Long> {
    Optional<MetalRateEntity> findByMetalAndRateDate(String metal, LocalDate rateDate);
    List<MetalRateEntity> findTop7ByMetalOrderByRateDateDesc(String metal);
}
