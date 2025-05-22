package com.crypto.platform.service;

import com.crypto.platform.model.CryptoPrice;
import com.crypto.platform.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HdfsService {

    private final CryptoPriceRepository cryptoPriceRepository;
    
    @Value("${hadoop.namenode.url}")
    private String hdfsUrl;
    
    @Value("${hadoop.hdfs.basePath}")
    private String hdfsBasePath;
    
    private Configuration hadoopConfig;
    
    @PostConstruct
    public void init() {
        // Initialize Hadoop configuration
        hadoopConfig = new Configuration();
        hadoopConfig.set("fs.defaultFS", hdfsUrl);
    }
    
    /**
     * Scheduled job to export hourly data to HDFS
     */
    @Scheduled(cron = "${scheduler.hdfs.import.cron}")
    public void exportDataToHdfs() {
        log.info("Starting HDFS export job at {}", Instant.now());
        
        try {
            // Get all distinct crypto symbols
            List<String> symbols = cryptoPriceRepository.findAllCryptoSymbols();
            
            // For each symbol, export the last hour of data
            for (String symbol : symbols) {
                exportSymbolDataToHdfs(symbol);
            }
            
            log.info("Completed HDFS export job");
        } catch (Exception e) {
            log.error("Error during HDFS export: {}", e.getMessage());
        }
    }
    
    /**
     * Export data for a specific symbol to HDFS
     */
    public void exportSymbolDataToHdfs(String symbol) throws IOException {
        // Calculate time range (last hour)
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);
        
        // Fetch data from repository
        List<CryptoPrice> prices = cryptoPriceRepository.findBySymbolAndTimeRange(symbol, startTime, endTime);
        
        if (prices.isEmpty()) {
            log.info("No data to export for symbol: {}", symbol);
            return;
        }
        
        // Create HDFS directory structure: /crypto/data/[symbol]/YYYY/MM/DD/HH
        String year = String.valueOf(java.time.LocalDateTime.now().getYear());
        String month = String.format("%02d", java.time.LocalDateTime.now().getMonthValue());
        String day = String.format("%02d", java.time.LocalDateTime.now().getDayOfMonth());
        String hour = String.format("%02d", java.time.LocalDateTime.now().getHour());
        
        String hdfsDir = String.format("%s/%s/%s/%s/%s/%s", 
                hdfsBasePath, symbol, year, month, day, hour);
        
        String hdfsFilePath = String.format("%s/prices.csv", hdfsDir);
        
        // Connect to HDFS
        try (FileSystem fs = FileSystem.get(hadoopConfig)) {
            // Create directories if they don't exist
            Path dirPath = new Path(hdfsDir);
            if (!fs.exists(dirPath)) {
                fs.mkdirs(dirPath);
            }
            
            // Create file and write data
            Path filePath = new Path(hdfsFilePath);
            try (FSDataOutputStream outputStream = fs.create(filePath, true);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                
                // Write header
                writer.write("id,symbol,price,volume24h,marketCap,high24h,low24h,priceChangePercent24h,timestamp");
                writer.newLine();
                
                // Write data rows
                for (CryptoPrice price : prices) {
                    writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                            price.getId(),
                            price.getSymbol(),
                            price.getPrice(),
                            price.getVolume24h(),
                            price.getMarketCap(),
                            price.getHigh24h(),
                            price.getLow24h(),
                            price.getPriceChangePercent24h(),
                            price.getTimestamp()));
                    writer.newLine();
                }
            }
            
            log.info("Successfully exported {} records for symbol {} to HDFS path: {}", 
                    prices.size(), symbol, hdfsFilePath);
        }
    }
}