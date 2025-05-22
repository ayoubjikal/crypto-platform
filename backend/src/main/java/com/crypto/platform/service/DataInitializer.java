package com.crypto.platform.service;

import com.crypto.platform.model.CryptoPrice;
import com.crypto.platform.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CryptoPriceRepository cryptoPriceRepository;
    private final BinanceService binanceService;
    private final WebClient.Builder webClientBuilder;

    @Value("${binance.api.base-url}")
    private String apiBaseUrl;

    // List of crypto symbols to track (should match the ones in BinanceService)
    private final List<String> symbols = Arrays.asList("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "DOGEUSDT");

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        log.info("Checking if initial data load is needed...");
        
        // Check if we have any data in the database
        long count = cryptoPriceRepository.count();
        
        if (count == 0) {
            log.info("No data found in database. Loading initial historical data...");
            loadHistoricalData();
        } else {
            log.info("Database already contains {} price records. Skipping initial data load.", count);
        }
    }
    
    private void loadHistoricalData() {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();
        
        // Get data for each symbol
        for (String symbol : symbols) {
            try {
                log.info("Loading historical data for {}", symbol);
                
                // First try to get current price to ensure everything works
                CryptoPrice latestPrice = binanceService.fetchAndSaveCryptoPrice(symbol);
                log.info("Latest price for {} saved: {}", symbol, latestPrice.getPrice());
                
                // Then try to get some historical klines/candlestick data
                // 1h intervals for the last 24 hours (24 data points)
                String interval = "1h";
                long endTime = System.currentTimeMillis();
                long startTime = endTime - (24 * 60 * 60 * 1000); // 24 hours ago
                
                String url = String.format("/api/v3/klines?symbol=%s&interval=%s&startTime=%d&endTime=%d&limit=24",
                        symbol, interval, startTime, endTime);
                
                List<List<Object>> klines = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(List.class)
                        .block();
                
                if (klines != null && !klines.isEmpty()) {
                    List<CryptoPrice> prices = new ArrayList<>();
                    
                    for (List<Object> kline : klines) {
                        // Kline format: [Open time, Open, High, Low, Close, Volume, Close time, ...]
                        long openTime = Long.parseLong(kline.get(0).toString());
                        BigDecimal closePrice = new BigDecimal(kline.get(4).toString());
                        BigDecimal volume = new BigDecimal(kline.get(5).toString());
                        BigDecimal high = new BigDecimal(kline.get(2).toString());
                        BigDecimal low = new BigDecimal(kline.get(3).toString());
                        
                        // For price change, we'll calculate a placeholder
                        BigDecimal open = new BigDecimal(kline.get(1).toString());
                        BigDecimal priceChangePercent = closePrice.subtract(open)
                                .divide(open, 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        
                        // For market cap, we'll use a placeholder calculation
                        BigDecimal marketCap = closePrice.multiply(volume)
                                .divide(BigDecimal.valueOf(1000), BigDecimal.ROUND_HALF_UP);
                        
                        CryptoPrice price = CryptoPrice.builder()
                                .symbol(symbol)
                                .price(closePrice)
                                .volume24h(volume)
                                .high24h(high)
                                .low24h(low)
                                .priceChangePercent24h(priceChangePercent)
                                .marketCap(marketCap)
                                .timestamp(Instant.ofEpochMilli(openTime))
                                .build();
                        
                        prices.add(price);
                    }
                    
                    // Save all prices
                    cryptoPriceRepository.saveAll(prices);
                    log.info("Saved {} historical data points for {}", prices.size(), symbol);
                }
                
                // Sleep a bit to avoid hitting rate limits
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("Error loading historical data for {}: {}", symbol, e.getMessage(), e);
            }
        }
        
        log.info("Initial data loading completed");
    }
}