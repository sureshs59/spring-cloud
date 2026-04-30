package com.goldexchange.fetcher.repository;

import com.goldexchange.fetcher.model.MetalRate;
import com.goldexchange.fetcher.model.MetalRate.MetalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetalRateRepository extends JpaRepository<MetalRate, Long> {

    Optional<MetalRate> findByRateDateAndMetal(LocalDate rateDate, MetalType metal);

    boolean existsByRateDateAndMetal(LocalDate rateDate, MetalType metal);

    List<MetalRate> findByMetalOrderByRateDateDesc(MetalType metal);

    @Query("SELECT r FROM MetalRate r WHERE r.metal = :metal AND r.rateDate >= :from ORDER BY r.rateDate ASC")
    List<MetalRate> findByMetalAndDateRange(@Param("metal") MetalType metal, @Param("from") LocalDate from);

    @Query("SELECT r FROM MetalRate r WHERE r.rateDate = (SELECT MAX(r2.rateDate) FROM MetalRate r2 WHERE r2.metal = :metal)")
    List<MetalRate> findLatestByMetal(@Param("metal") MetalType metal);

    @Query("SELECT r FROM MetalRate r WHERE r.rateDate >= :from ORDER BY r.rateDate DESC")
    List<MetalRate> findAllSince(@Param("from") LocalDate from);
}
