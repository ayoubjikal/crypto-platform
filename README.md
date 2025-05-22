# Cryptocurrency Data Visualization and Forecasting Platform

A real-time platform for visualizing and forecasting cryptocurrency prices using Big Data tools.

## Architecture

This platform integrates:
- Spring Boot for the web backend and API
- React for the frontend
- PostgreSQL for recent data storage
- Hadoop HDFS for historical data storage
- Sqoop for data transfer between RDBMS and HDFS
- Apache Spark for big data processing and ML models
- Binance API for real-time cryptocurrency data

## Components

### Backend

The Spring Boot backend provides:
- REST API for crypto price data
- User authentication and authorization
- Integration with Binance API
- Scheduled jobs for data collection
- Interaction with Hadoop/HDFS through native Java API
- Interaction with Spark for data processing and ML

### Frontend

The React frontend provides:
- Real-time price display
- Price charts and visualizations
- Historical data view
- Price predictions and analytics
- User registration and login

### Big Data Stack

The Big Data components include:
- Hadoop HDFS for distributed storage of historical data
- Sqoop for importing/exporting data between PostgreSQL and HDFS
- Apache Spark for batch processing and machine learning
- Jupyter Notebook for ML development and exploration

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Binance API key (optional for enhanced features)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/crypto-platform.git
   cd crypto-platform
   ```

2. Configure environment variables (optional):
   ```bash
   cp .env.example .env
   # Edit .env to add your Binance API key if available
   ```

3. Start the application:
   ```bash
   docker-compose up -d
   ```

4. Access the application:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - Hadoop NameNode UI: http://localhost:9870
   - Spark Master UI: http://localhost:8181
   - Jupyter Notebook: http://localhost:8888

### Initial Setup

On first run, the application will:
1. Create necessary database tables
2. Create initial admin user (username: admin, password: admin123)
3. Start fetching crypto data from Binance API

## Development

### Backend Development

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Build with Maven:
   ```bash
   mvn clean install
   ```

### Frontend Development

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start development server:
   ```bash
   npm start
   ```

### Big Data Development

- Hadoop configuration files are in `hadoop/etc/hadoop/`
- Spark applications are in `spark/apps/`
- Sqoop scripts are in `sqoop/scripts/`

## Data Flow

1. Real-time data is fetched from Binance API and stored in PostgreSQL
2. Scheduled Sqoop jobs transfer data from PostgreSQL to HDFS
3. Spark batch jobs process historical data in HDFS
4. Machine learning models generate price predictions
5. Frontend displays both real-time and processed data

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.