package com.crypto.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal predictedPrice;

    @Column(nullable = false)
    private BigDecimal confidenceInterval;

    @Column(nullable = false)
    private Instant targetDate;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private String model;  // ML model used for prediction

    @Column
    private BigDecimal accuracy;  // Model accuracy

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}