package com.crypto.platform.repository;

import com.crypto.platform.model.CryptoPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {

    Optional<CryptoPrice> findTopBySymbolOrderByTimestampDesc(String symbol);

    List<CryptoPrice> findBySymbolOrderByTimestampDesc(String symbol, Pageable pageable);

    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.symbol = :symbol AND cp.timestamp BETWEEN :startTime AND :endTime ORDER BY cp.timestamp ASC")
    List<CryptoPrice> findBySymbolAndTimeRange(
            @Param("symbol") String symbol,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT DISTINCT cp.symbol FROM CryptoPrice cp")
    List<String> findAllCryptoSymbols();
}