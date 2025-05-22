package com.crypto.platform.repository;

import com.crypto.platform.model.PricePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PricePredictionRepository extends JpaRepository<PricePrediction, Long> {

    Optional<PricePrediction> findTopBySymbolOrderByCreatedAtDesc(String symbol);

    List<PricePrediction> findBySymbolOrderByTargetDateAsc(String symbol);

    @Query("SELECT pp FROM PricePrediction pp WHERE pp.symbol = :symbol AND pp.targetDate BETWEEN :startDate AND :endDate ORDER BY pp.targetDate ASC")
    List<PricePrediction> findBySymbolAndTargetDateRange(
            @Param("symbol") String symbol,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}