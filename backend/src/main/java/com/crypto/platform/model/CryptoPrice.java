package com.crypto.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "crypto_prices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal volume24h;

    @Column(nullable = false)
    private BigDecimal marketCap;

    @Column(nullable = false)
    private BigDecimal high24h;

    @Column(nullable = false)
    private BigDecimal low24h;

    @Column(nullable = false)
    private BigDecimal priceChangePercent24h;

    @Column(nullable = false)
    private Instant timestamp;
}