package com.crypto.platform.controller;

import com.crypto.platform.model.CryptoPrice;
import com.crypto.platform.repository.CryptoPriceRepository;
import com.crypto.platform.service.BinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class CryptoPriceController {

    private final CryptoPriceRepository cryptoPriceRepository;
    private final BinanceService binanceService;

    /**
     * Get latest price for a symbol
     */
    @GetMapping("/{symbol}/latest")
    public ResponseEntity<CryptoPrice> getLatestPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(binanceService.getLatestPrice(symbol));
    }

    /**
     * Fetch recent prices for a symbol
     */
    @GetMapping("/{symbol}/recent")
    public ResponseEntity<List<CryptoPrice>> getRecentPrices(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        List<CryptoPrice> prices = cryptoPriceRepository.findBySymbolOrderByTimestampDesc(symbol, pageable);
        
        return ResponseEntity.ok(prices);
    }

    /**
     * Get prices for a symbol in a specific time range
     */
    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<CryptoPrice>> getPriceHistory(
            @PathVariable String symbol,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime) {
        
        // If not specified, use last 24 hours
        if (startTime == null) {
            startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        List<CryptoPrice> prices = cryptoPriceRepository.findBySymbolAndTimeRange(symbol, startTime, endTime);
        
        return ResponseEntity.ok(prices);
    }

    /**
     * Force refresh of price data (admin only)
     */
    @PostMapping("/{symbol}/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CryptoPrice> refreshPrice(@PathVariable String symbol) {
        CryptoPrice price = binanceService.fetchAndSaveCryptoPrice(symbol);
        return ResponseEntity.ok(price);
    }

    /**
     * Get all available crypto symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAllSymbols() {
        List<String> symbols = cryptoPriceRepository.findAllCryptoSymbols();
        return ResponseEntity.ok(symbols);
    }
}