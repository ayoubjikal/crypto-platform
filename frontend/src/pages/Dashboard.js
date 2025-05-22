import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { 
  Typography, 
  Grid, 
  Card, 
  CardContent, 
  CardActions, 
  Button,
  CircularProgress,
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert
} from '@mui/material';
import ShowChartIcon from '@mui/icons-material/ShowChart';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';

const Dashboard = () => {
  const [cryptoData, setCryptoData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const symbols = ['BTCUSDT', 'ETHUSDT', 'BNBUSDT', 'ADAUSDT', 'DOGEUSDT'];
    
    const fetchCryptoData = async () => {
      setLoading(true);
      try {
        const promises = symbols.map(symbol => 
          axios.get(`/api/prices/${symbol}/latest`)
        );
        
        const responses = await Promise.all(promises);
        const data = responses.map(res => res.data);
        
        setCryptoData(data);
        setError(null);
      } catch (err) {
        console.error('Error fetching crypto data:', err);
        setError('Failed to fetch cryptocurrency data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchCryptoData();
    
    // Refresh data every 30 seconds
    const interval = setInterval(fetchCryptoData, 30000);
    
    return () => clearInterval(interval);
  }, []);

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

  return (
    <div>
      <Typography variant="h4" component="h1" gutterBottom>
        Cryptocurrency Dashboard
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Grid container spacing={3} sx={{ mb: 4 }}>
            {cryptoData.map((crypto) => (
              <Grid item xs={12} sm={6} md={4} key={crypto.symbol}>
                <Card>
                  <CardContent>
                    <Typography variant="h5" component="div">
                      {crypto.symbol}
                    </Typography>
                    <Typography variant="h4" sx={{ mt: 2 }}>
                      {formatPrice(crypto.price)}
                    </Typography>
                    <Box 
                      sx={{ 
                        display: 'flex', 
                        alignItems: 'center',
                        color: crypto.priceChangePercent24h >= 0 ? 'success.main' : 'error.main'
                      }}
                    >
                      {crypto.priceChangePercent24h >= 0 ? 
                        <TrendingUpIcon sx={{ mr: 1 }} /> : 
                        <TrendingDownIcon sx={{ mr: 1 }} />
                      }
                      <Typography variant="body1">
                        {formatPercent(crypto.priceChangePercent24h)}
                      </Typography>
                    </Box>
                  </CardContent>
                  <CardActions>
                    <Button 
                      size="small" 
                      component={Link} 
                      to={`/crypto/${crypto.symbol}`}
                      startIcon={<ShowChartIcon />}
                    >
                      View Details
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
          
          <Typography variant="h5" component="h2" gutterBottom>
            Market Overview
          </Typography>
          
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Symbol</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell align="right">24h Change</TableCell>
                  <TableCell align="right">24h Volume</TableCell>
                  <TableCell align="right">Market Cap</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {cryptoData.map((crypto) => (
                  <TableRow key={crypto.symbol}>
                    <TableCell component="th" scope="row">
                      <Link to={`/crypto/${crypto.symbol}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                        {crypto.symbol}
                      </Link>
                    </TableCell>
                    <TableCell align="right">{formatPrice(crypto.price)}</TableCell>
                    <TableCell 
                      align="right"
                      sx={{ 
                        color: crypto.priceChangePercent24h >= 0 ? 'success.main' : 'error.main'
                      }}
                    >
                      {formatPercent(crypto.priceChangePercent24h)}
                    </TableCell>
                    <TableCell align="right">{formatPrice(crypto.volume24h)}</TableCell>
                    <TableCell align="right">{formatPrice(crypto.marketCap)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </div>
  );
};

export default Dashboard;