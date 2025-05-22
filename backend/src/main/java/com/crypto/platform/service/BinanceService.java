package com.crypto.platform.service;

import com.crypto.platform.model.CryptoPrice;
import com.crypto.platform.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceService {

    private final CryptoPriceRepository cryptoPriceRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${binance.api.base-url}")
    private String apiBaseUrl;

    // List of crypto symbols to track
    private final List<String> symbols = Arrays.asList("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "DOGEUSDT");

    /**
     * Scheduled job to fetch crypto prices from Binance API
     */
    @Scheduled(cron = "${scheduler.binance.data.fetch.cron}")
    public void fetchCryptoPrices() {
        log.info("Fetching crypto prices from Binance API at {}", Instant.now());
        
        for (String symbol : symbols) {
            try {
                fetchAndSaveCryptoPrice(symbol);
            } catch (Exception e) {
                log.error("Error fetching price for symbol {}: {}", symbol, e.getMessage());
            }
        }
    }

    /**
     * Fetch crypto price for a specific symbol and save it to the database
     */
    public CryptoPrice fetchAndSaveCryptoPrice(String symbol) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();
        
        // Fetch ticker price
        Map<String, Object> tickerResponse = webClient.get()
                .uri("/api/v3/ticker/24hr?symbol=" + symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        if (tickerResponse == null) {
            throw new RuntimeException("Failed to fetch data from Binance API");
        }
        
        // Extract required fields
        BigDecimal price = new BigDecimal(tickerResponse.get("lastPrice").toString());
        BigDecimal volume = new BigDecimal(tickerResponse.get("volume").toString());
        BigDecimal high24h = new BigDecimal(tickerResponse.get("highPrice").toString());
        BigDecimal low24h = new BigDecimal(tickerResponse.get("lowPrice").toString());
        BigDecimal priceChangePercent = new BigDecimal(tickerResponse.get("priceChangePercent").toString());
        
        // For market cap, we'll just use a placeholder since Binance doesn't provide this directly
        BigDecimal marketCap = price.multiply(volume).divide(BigDecimal.valueOf(1000), BigDecimal.ROUND_HALF_UP);
        
        // Create and save the entity
        CryptoPrice cryptoPrice = CryptoPrice.builder()
                .symbol(symbol)
                .price(price)
                .volume24h(volume)
                .high24h(high24h)
                .low24h(low24h)
                .priceChangePercent24h(priceChangePercent)
                .marketCap(marketCap)
                .timestamp(Instant.now())
                .build();
        
        log.info("Saving price for {}: {}", symbol, price);
        return cryptoPriceRepository.save(cryptoPrice);
    }

    /**
     * Get the latest price for a symbol
     * If no price is found in the database, fetch it from Binance API
     */
    public CryptoPrice getLatestPrice(String symbol) {
        return cryptoPriceRepository.findTopBySymbolOrderByTimestampDesc(symbol)
                .orElseGet(() -> {
                    log.info("No price data found in DB for symbol: {}. Fetching from Binance API...", symbol);
                    try {
                        return fetchAndSaveCryptoPrice(symbol);
                    } catch (Exception e) {
                        log.error("Error fetching price for symbol {} from API: {}", symbol, e.getMessage());
                        throw new RuntimeException("Could not fetch price data for symbol: " + symbol, e);
                    }
                });
    }
}