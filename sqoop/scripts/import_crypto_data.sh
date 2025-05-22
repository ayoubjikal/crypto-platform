#!/bin/bash

# This script imports cryptocurrency price data from PostgreSQL to HDFS
# It is designed to be run as a scheduled job

# Environment variables (set in Docker container)
HADOOP_NAMENODE=${HADOOP_NAMENODE:-hadoop-namenode}
POSTGRES_HOST=${POSTGRES_HOST:-postgres}
POSTGRES_DB=${POSTGRES_DB:-cryptodb}
POSTGRES_USER=${POSTGRES_USER:-postgres}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-postgres}

# HDFS path
HDFS_BASE_PATH=/crypto/data

# Get current date components for directory structure
YEAR=$(date +"%Y")
MONTH=$(date +"%m")
DAY=$(date +"%d")
HOUR=$(date +"%H")

echo "Starting Sqoop import job at $(date)"

# Get list of crypto symbols from database
SYMBOLS=$(sqoop eval \
  --connect jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
  --username ${POSTGRES_USER} \
  --password ${POSTGRES_PASSWORD} \
  --query "SELECT DISTINCT symbol FROM crypto_prices" | grep -o "BTCUSDT\|ETHUSDT\|BNBUSDT\|ADAUSDT\|DOGEUSDT")

# Loop through each symbol and import data
for SYMBOL in ${SYMBOLS}; do
  echo "Importing data for symbol: ${SYMBOL}"
  
  # Create the HDFS directory structure if it doesn't exist
  HDFS_DIR="${HDFS_BASE_PATH}/${SYMBOL}/${YEAR}/${MONTH}/${DAY}/${HOUR}"
  hdfs dfs -mkdir -p ${HDFS_DIR}
  
  # Run Sqoop import
  sqoop import \
    --connect jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
    --username ${POSTGRES_USER} \
    --password ${POSTGRES_PASSWORD} \
    --table crypto_prices \
    --where "symbol='${SYMBOL}'" \
    --columns "id,symbol,price,volume24h,market_cap,high24h,low24h,price_change_percent24h,timestamp" \
    --target-dir ${HDFS_DIR} \
    --fields-terminated-by ',' \
    --lines-terminated-by '\n' \
    --as-textfile \
    --direct \
    --m 1
  
  # Check if the import was successful
  if [ $? -eq 0 ]; then
    echo "Successfully imported ${SYMBOL} data to HDFS: ${HDFS_DIR}"
  else
    echo "Error importing ${SYMBOL} data to HDFS"
  fi
done

echo "Sqoop import job completed at $(date)"