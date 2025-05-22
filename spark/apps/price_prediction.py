#!/usr/bin/env python3

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, lit, avg, max, min, stddev, count, expr
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, TimestampType
from pyspark.ml.feature import VectorAssembler
from pyspark.ml.regression import LinearRegression
from pyspark.ml.evaluation import RegressionEvaluator
import time
import datetime
import requests
import json

# Constants
HDFS_BASE_PATH = "/crypto/data"
API_BASE_URL = "http://backend:8080/api"
SYMBOLS = ["BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "DOGEUSDT"]

def main():
    # Initialize Spark session
    spark = SparkSession.builder \
        .appName("Crypto Price Prediction") \
        .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
    
    print("Starting price prediction job")
    
    for symbol in SYMBOLS:
        try:
            predict_price_for_symbol(spark, symbol)
        except Exception as e:
            print(f"Error processing {symbol}: {e}")
    
    print("Price prediction job completed")
    spark.stop()

def predict_price_for_symbol(spark, symbol):
    print(f"Processing symbol: {symbol}")
    
    # Load historical data from HDFS
    df = load_historical_data(spark, symbol)
    
    if df is None or df.count() < 10:
        print(f"Not enough data for {symbol} to make a prediction")
        return
    
    # Prepare dataset for ML
    ml_df = prepare_data_for_ml(df)
    
    # Train model and make predictions
    predictions = train_and_predict(spark, ml_df, symbol)
    
    # Save predictions to the backend API
    save_predictions(predictions, symbol)

def load_historical_data(spark, symbol):
    """Load and combine historical data from HDFS"""
    try:
        # The full data path includes all subdirectories
        data_path = f"{HDFS_BASE_PATH}/{symbol}/*/*/*/*.csv"
        
        # Define schema for CSV data
        schema = StructType([
            StructField("id", StringType(), True),
            StructField("symbol", StringType(), True),
            StructField("price", DoubleType(), True),
            StructField("volume24h", DoubleType(), True),
            StructField("market_cap", DoubleType(), True),
            StructField("high24h", DoubleType(), True),
            StructField("low24h", DoubleType(), True),
            StructField("price_change_percent24h", DoubleType(), True),
            StructField("timestamp", TimestampType(), True)
        ])
        
        # Load data
        df = spark.read.csv(data_path, header=True, schema=schema)
        
        print(f"Loaded {df.count()} records for {symbol}")
        return df
        
    except Exception as e:
        print(f"Error loading data for {symbol}: {e}")
        return None

def prepare_data_for_ml(df):
    """Prepare data for machine learning by creating features and label"""
    # Calculate basic features
    df = df.withColumn("hour_of_day", expr("hour(timestamp)"))
    df = df.withColumn("day_of_week", expr("dayofweek(timestamp)"))
    
    # Create target variable (next price)
    # This is a simplified approach; in reality we'd use a more sophisticated time series method
    df = df.withColumn("price_lagged", col("price"))
    
    # Select features and label
    feature_cols = ["price_lagged", "volume24h", "high24h", "low24h", "price_change_percent24h", 
                   "hour_of_day", "day_of_week"]
    
    # Assemble features into a vector
    assembler = VectorAssembler(inputCols=feature_cols, outputCol="features")
    ml_df = assembler.transform(df).select("features", col("price").alias("label"))
    
    return ml_df

def train_and_predict(spark, ml_df, symbol):
    """Train a model and make predictions"""
    # Split data into training and testing sets
    train_df, test_df = ml_df.randomSplit([0.8, 0.2], seed=42)
    
    # Create and train the model
    lr = LinearRegression(maxIter=10, regParam=0.3, elasticNetParam=0.8)
    model = lr.fit(train_df)
    
    # Make predictions on test data
    predictions = model.transform(test_df)
    
    # Evaluate the model
    evaluator = RegressionEvaluator(labelCol="label", predictionCol="prediction", metricName="rmse")
    rmse = evaluator.evaluate(predictions)
    print(f"Root Mean Squared Error (RMSE) for {symbol}: {rmse}")
    
    # Calculate confidence interval (simplified)
    confidence = rmse
    
    # Now make predictions for future dates
    latest_price = ml_df.select(col("label")).orderBy(col("label").desc()).first()[0]
    
    # Make predictions for 1, 7, and 30 days ahead
    future_predictions = []
    
    # Very simple prediction model for demonstration
    # In a real scenario, we would use a proper time series model
    for days_ahead in [1, 7, 30]:
        # Simple prediction based on current price and model insight
        # This is just a placeholder for demonstration
        prediction = latest_price * (1 + 0.01 * days_ahead) 
        
        target_date = datetime.datetime.now() + datetime.timedelta(days=days_ahead)
        
        future_predictions.append({
            "symbol": symbol,
            "predictedPrice": prediction,
            "confidenceInterval": confidence,
            "targetDate": target_date.isoformat(),
            "model": "LinearRegression"
        })
    
    return future_predictions

def save_predictions(predictions, symbol):
    """Save predictions to the backend API"""
    for prediction in predictions:
        try:
            response = requests.post(
                f"{API_BASE_URL}/predictions/{symbol}/save",
                json=prediction
            )
            
            if response.status_code == 200:
                print(f"Successfully saved prediction for {symbol}, target date: {prediction['targetDate']}")
            else:
                print(f"Error saving prediction: {response.status_code} - {response.text}")
                
        except Exception as e:
            print(f"Error making API request: {e}")

if __name__ == "__main__":
    main()