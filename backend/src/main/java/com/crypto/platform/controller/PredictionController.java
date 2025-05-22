package com.crypto.platform.controller;

import com.crypto.platform.model.PricePrediction;
import com.crypto.platform.repository.PricePredictionRepository;
import com.crypto.platform.service.SparkMLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PricePredictionRepository pricePredictionRepository;
    private final SparkMLService sparkMLService;

    /**
     * Get latest prediction for a symbol
     */
    @GetMapping("/{symbol}/latest")
    public ResponseEntity<PricePrediction> getLatestPrediction(@PathVariable String symbol) {
        return pricePredictionRepository.findTopBySymbolOrderByCreatedAtDesc(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all predictions for a symbol
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<List<PricePrediction>> getPredictions(@PathVariable String symbol) {
        List<PricePrediction> predictions = pricePredictionRepository.findBySymbolOrderByTargetDateAsc(symbol);
        return ResponseEntity.ok(predictions);
    }

    /**
     * Get predictions for a specific date range
     */
    @GetMapping("/{symbol}/range")
    public ResponseEntity<List<PricePrediction>> getPredictionsInRange(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        
        // If not specified, use next 30 days
        if (startDate == null) {
            startDate = Instant.now();
        }
        
        if (endDate == null) {
            endDate = Instant.now().plus(30, ChronoUnit.DAYS);
        }
        
        List<PricePrediction> predictions = pricePredictionRepository.findBySymbolAndTargetDateRange(
                symbol, startDate, endDate);
        
        return ResponseEntity.ok(predictions);
    }

    /**
     * Force a new prediction for a symbol (admin only)
     */
    @PostMapping("/{symbol}/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> refreshPrediction(@PathVariable String symbol) {
        try {
            sparkMLService.predictPriceForSymbol(symbol);
            return ResponseEntity.ok("Prediction job started for symbol: " + symbol);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}