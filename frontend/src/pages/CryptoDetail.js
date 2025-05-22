import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';
import { 
  Typography, 
  Grid, 
  Card, 
  CardContent,
  CircularProgress,
  Box,
  Alert,
  Tabs,
  Tab,
  Divider,
  Paper
} from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import 'chartjs-adapter-date-fns';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

const CryptoDetail = () => {
  const { symbol } = useParams();
  const [cryptoData, setCryptoData] = useState(null);
  const [historicalData, setHistoricalData] = useState([]);
  const [predictions, setPredictions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch current data
        const currentResponse = await axios.get(`/api/prices/${symbol}/latest`);
        setCryptoData(currentResponse.data);

        // Fetch historical data
        const historyResponse = await axios.get(`/api/prices/${symbol}/recent?limit=100`);
        setHistoricalData(historyResponse.data);

        // Fetch predictions
        const predictionsResponse = await axios.get(`/api/predictions/${symbol}`);
        setPredictions(predictionsResponse.data);
        
        setError(null);
      } catch (err) {
        console.error('Error fetching crypto data:', err);
        setError('Failed to fetch cryptocurrency data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    if (symbol) {
      fetchData();
    }
    
    // Refresh data every minute
    const interval = setInterval(() => {
      if (symbol) {
        fetchData();
      }
    }, 60000);
    
    return () => clearInterval(interval);
  }, [symbol]);

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(price);
  };

  const formatPercent = (percent) => {
    return new Intl.NumberFormat('en-US', {
      style: 'percent',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(percent / 100);
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  // Prepare chart data
  const prepareChartData = () => {
    // Sort historical data by timestamp
    const sortedData = [...historicalData].sort((a, b) => 
      new Date(a.timestamp) - new Date(b.timestamp)
    );

    // Format data for Chart.js
    return {
      labels: sortedData.map(item => new Date(item.timestamp)),
      datasets: [
        {
          label: `${symbol} Price`,
          data: sortedData.map(item => item.price),
          borderColor: 'rgba(25, 118, 210, 1)',
          backgroundColor: 'rgba(25, 118, 210, 0.2)',
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 5,
          tension: 0.1
        }
      ]
    };
  };

  // Prepare prediction chart data
  const preparePredictionChartData = () => {
    // If we have historical data and predictions
    if (historicalData.length === 0 || predictions.length === 0) {
      return null;
    }

    // Sort historical data by timestamp
    const sortedHistorical = [...historicalData]
      .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
      .slice(-20); // Take only the last 20 points
    
    // Sort predictions by target date
    const sortedPredictions = [...predictions]
      .sort((a, b) => new Date(a.targetDate) - new Date(b.targetDate));
    
    // Get the last actual price
    const lastActualPrice = sortedHistorical[sortedHistorical.length - 1].price;
    const lastActualDate = new Date(sortedHistorical[sortedHistorical.length - 1].timestamp);
    
    // Format data for Chart.js
    return {
      labels: [
        ...sortedHistorical.map(item => new Date(item.timestamp)),
        ...sortedPredictions.map(item => new Date(item.targetDate))
      ],
      datasets: [
        {
          label: 'Actual Price',
          data: [...sortedHistorical.map(item => item.price), null],
          borderColor: 'rgba(25, 118, 210, 1)',
          backgroundColor: 'rgba(25, 118, 210, 0.2)',
          borderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 5,
          tension: 0.1
        },
        {
          label: 'Predicted Price',
          data: [
            ...sortedHistorical.map(() => null),
            ...sortedPredictions.map(item => item.predictedPrice)
          ],
          borderColor: 'rgba(233, 30, 99, 1)',
          backgroundColor: 'rgba(233, 30, 99, 0.2)',
          borderWidth: 2,
          borderDash: [5, 5],
          pointRadius: 3,
          tension: 0.1
        }
      ]
    };
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        type: 'time',
        time: {
          unit: 'hour'
        },
        title: {
          display: true,
          text: 'Time'
        }
      },
      y: {
        title: {
          display: true,
          text: 'Price (USD)'
        }
      }
    },
    plugins: {
      legend: {
        position: 'top',
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            return formatPrice(context.parsed.y);
          }
        }
      }
    }
  };

  return (
    <div>
      <Typography variant="h4" component="h1" gutterBottom>
        {symbol} Details
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      {loading && !cryptoData ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      ) : cryptoData && (
        <>
          <Grid container spacing={3} sx={{ mb: 4 }}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" color="text.secondary" gutterBottom>
                    Current Price
                  </Typography>
                  <Typography variant="h3" component="div">
                    {formatPrice(cryptoData.price)}
                  </Typography>
                  <Box 
                    sx={{ 
                      display: 'flex', 
                      alignItems: 'center',
                      color: cryptoData.priceChangePercent24h >= 0 ? 'success.main' : 'error.main',
                      mt: 1
                    }}
                  >
                    {cryptoData.priceChangePercent24h >= 0 ? 
                      <TrendingUpIcon sx={{ mr: 1 }} /> : 
                      <TrendingDownIcon sx={{ mr: 1 }} />
                    }
                    <Typography variant="body1">
                      {formatPercent(cryptoData.priceChangePercent24h)} (24h)
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                    Last updated: {formatDate(cryptoData.timestamp)}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" color="text.secondary" gutterBottom>
                    Market Data
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        24h Volume
                      </Typography>
                      <Typography variant="h6">
                        {formatPrice(cryptoData.volume24h)}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        Market Cap
                      </Typography>
                      <Typography variant="h6">
                        {formatPrice(cryptoData.marketCap)}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        24h High
                      </Typography>
                      <Typography variant="h6">
                        {formatPrice(cryptoData.high24h)}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        24h Low
                      </Typography>
                      <Typography variant="h6">
                        {formatPrice(cryptoData.low24h)}
                      </Typography>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
          
          <Paper sx={{ width: '100%', mb: 4 }}>
            <Tabs
              value={activeTab}
              onChange={handleTabChange}
              indicatorColor="primary"
              textColor="primary"
              centered
            >
              <Tab label="Price Chart" />
              <Tab label="Predictions" />
            </Tabs>
            <Divider />
            
            {activeTab === 0 && (
              <Box sx={{ p: 3, height: 400 }}>
                {historicalData.length > 0 ? (
                  <Line data={prepareChartData()} options={chartOptions} />
                ) : (
                  <Typography variant="body1" sx={{ textAlign: 'center', mt: 8 }}>
                    No historical data available
                  </Typography>
                )}
              </Box>
            )}
            
            {activeTab === 1 && (
              <Box sx={{ p: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Price Predictions
                </Typography>
                
                {predictions.length > 0 ? (
                  <>
                    <Box sx={{ height: 400, mb: 4 }}>
                      <Line data={preparePredictionChartData()} options={chartOptions} />
                    </Box>
                    
                    <Grid container spacing={2}>
                      {predictions.map((prediction, index) => (
                        <Grid item xs={12} sm={4} key={index}>
                          <Card variant="outlined">
                            <CardContent>
                              <Typography variant="body2" color="text.secondary">
                                Target Date
                              </Typography>
                              <Typography variant="body1" gutterBottom>
                                {formatDate(prediction.targetDate)}
                              </Typography>
                              
                              <Typography variant="body2" color="text.secondary">
                                Predicted Price
                              </Typography>
                              <Typography variant="h6" gutterBottom>
                                {formatPrice(prediction.predictedPrice)}
                              </Typography>
                              
                              <Typography variant="body2" color="text.secondary">
                                Confidence
                              </Typography>
                              <Typography variant="body1">
                                ±{formatPrice(prediction.confidenceInterval)}
                              </Typography>
                            </CardContent>
                          </Card>
                        </Grid>
                      ))}
                    </Grid>
                  </>
                ) : (
                  <Typography variant="body1" sx={{ textAlign: 'center', my: 4 }}>
                    No predictions available for this cryptocurrency
                  </Typography>
                )}
              </Box>
            )}
          </Paper>
        </>
      )}
    </div>
  );
};

export default CryptoDetail;