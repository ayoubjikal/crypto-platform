package com.crypto.platform.service;

import com.crypto.platform.model.PricePrediction;
import com.crypto.platform.repository.PricePredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SparkMLService {

    private final PricePredictionRepository pricePredictionRepository;
    
    @Value("${spark.master.url}")
    private String sparkMasterUrl;
    
    @Value("${spark.app.name}")
    private String sparkAppName;
    
    @Value("${hadoop.hdfs.basePath}")
    private String hdfsBasePath;
    
    private SparkSession spark;
    
    // List of crypto symbols to analyze
    private final List<String> symbols = Arrays.asList("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "DOGEUSDT");
    
    @PostConstruct
    public void init() {
        // Initialize Spark session
        spark = SparkSession.builder()
                .appName(sparkAppName)
                .master(sparkMasterUrl)
                .config("spark.executor.memory", "1g")
                .config("spark.driver.memory", "1g")
                .getOrCreate();
        
        log.info("Spark session initialized with master URL: {}", sparkMasterUrl);
    }
    
    @PreDestroy
    public void cleanup() {
        if (spark != null) {
            spark.close();
            log.info("Spark session closed");
        }
    }
    
    /**
     * Scheduled job to run ML prediction for crypto prices
     */
    @Scheduled(cron = "${scheduler.analytics.cron}")
    public void runPricePredictions() {
        log.info("Starting Spark ML predictions job at {}", Instant.now());
        
        for (String symbol : symbols) {
            try {
                predictPriceForSymbol(symbol);
            } catch (Exception e) {
                log.error("Error running prediction for symbol {}: {}", symbol, e.getMessage());
            }
        }
        
        log.info("Completed Spark ML predictions job");
    }
    
    /**
     * Predict future price for a specific crypto symbol
     */
    public void predictPriceForSymbol(String symbol) {
        log.info("Running prediction for symbol: {}", symbol);
        
        try {
            // Construct path to HDFS data for this symbol
            String dataPath = String.format("%s/%s", hdfsBasePath, symbol);
            
            // Load historical data from HDFS
            Dataset<Row> dataFrame = spark.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .csv(dataPath + "/*/*/*/*.csv");
            
            // Register as a temporary view for SQL queries
            dataFrame.createOrReplaceTempView("crypto_prices");
            
            // Simple analysis - calculate statistics and forecasts
            // In a real application, this would be a more sophisticated ML model
            Dataset<Row> result = spark.sql(
                    "SELECT " +
                    "   symbol, " +
                    "   AVG(price) as avg_price, " +
                    "   MAX(price) as max_price, " +
                    "   MIN(price) as min_price, " +
                    "   STDDEV(price) as stddev_price " +
                    "FROM crypto_prices " +
                    "WHERE symbol = '" + symbol + "' " +
                    "GROUP BY symbol"
            );
            
            // If we have results, create predictions
            if (!result.isEmpty()) {
                Row stats = result.first();
                
                // Calculate a simple forecast based on moving average
                // (this is a placeholder for real ML prediction)
                double avgPrice = stats.getDouble(stats.fieldIndex("avg_price"));
                double stdDevPrice = stats.getDouble(stats.fieldIndex("stddev_price"));
                
                // Create predictions for next day, week and month
                createPrediction(symbol, avgPrice, stdDevPrice, 1);
                createPrediction(symbol, avgPrice * 1.01, stdDevPrice * 1.2, 7);
                createPrediction(symbol, avgPrice * 1.03, stdDevPrice * 1.5, 30);
                
                log.info("Predictions created for symbol: {}", symbol);
            } else {
                log.warn("No data available for prediction for symbol: {}", symbol);
            }
            
        } catch (Exception e) {
            log.error("Error in Spark prediction for symbol {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Prediction failed", e);
        }
    }
    
    /**
     * Helper method to create and save a price prediction
     */
    private void createPrediction(String symbol, double predictedPrice, double stdDev, int daysAhead) {
        // Create prediction object
        PricePrediction prediction = PricePrediction.builder()
                .symbol(symbol)
                .predictedPrice(BigDecimal.valueOf(predictedPrice))
                .confidenceInterval(BigDecimal.valueOf(stdDev))
                .targetDate(Instant.now().plus(daysAhead, ChronoUnit.DAYS))
                .model("SimpleMovingAverage")
                .accuracy(BigDecimal.valueOf(0.85))  // placeholder
                .build();
        
        // Save to database
        pricePredictionRepository.save(prediction);
    }
}