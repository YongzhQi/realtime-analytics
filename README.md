# Real-time Analytics System

Real-time analytics platform processing 1900+ events per second with S3 archival.

## Architecture

- **Ingestion Service** - Spring Boot REST API (port 8081) that accepts events and publishes to Kafka
- **Processor Service** - Spring Boot consumer (port 8082) that processes Kafka events and stores to PostgreSQL + S3
- **S3 Archival** - Built-in AWS S3 event archiver with automatic batching and date-based organization
- **Infrastructure** - Docker Compose setup with Kafka (KRaft mode), PostgreSQL, and health monitoring
- **Dashboard** - React frontend with real-time metrics via Server-Sent Events

## Performance

- Throughput: 1900+ events/second (95% of 2000 target)
- Latency: Sub-second end-to-end processing
- Scalability: 6 Kafka partitions with batch processing (1000 records/poll)
- Storage: PostgreSQL + S3 with connection pooling and batch operations

## Status

- Ingestion Service: REST API accepting events, publishing to Kafka
- Processor Service: Consuming from Kafka, storing to PostgreSQL + S3
- S3 Integration: Archiving to `s3://realtime-analytics-yongqi`
- Dashboard: Real-time metrics via Server-Sent Events

## Quick Start

### 1. Infrastructure

```bash
docker compose -f infra/docker-compose.yml up -d
```

Create Kafka topic:
```bash
docker compose -f infra/docker-compose.yml exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic events --partitions 6 --replication-factor 1
```

### 2. Services

Start processor service:
```bash
cd processor-service && ./gradlew bootRun
```

Start ingestion service (in another terminal):
```bash
cd ingestion-service && ./gradlew bootRun
```

Services will be available on:
- Ingestion API: `http://localhost:8081`
- Processor Health: `http://localhost:8082/actuator/health`
- Metrics Stream: `http://localhost:8082/metrics/stream`

### 3. Testing

Send a test event:
```bash
curl -X POST http://localhost:8081/events \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"test-1","eventType":"page_view","payload":"{\"path\":\"/home\"}"}'
```

Monitor metrics stream:
```bash
curl -N http://localhost:8082/metrics/stream
```

### 4. Load Testing

Run performance test:
```bash
npm install
node load-test.js
```

### 5. Dashboard

```bash
cd dashboard
npm install
npm run dev
# Open http://localhost:5173
```

### 6. S3 Archival

The processor service includes S3 archival functionality.

#### Setup AWS Credentials

Create `.env` file in the project root:
```bash
# AWS Credentials for S3 Archival
AWS_ACCESS_KEY_ID=your_access_key_here
AWS_SECRET_ACCESS_KEY=your_secret_key_here
AWS_DEFAULT_REGION=us-east-2
```

#### Enable S3 Archival

```bash
cd processor-service
S3_ENABLED=true ./gradlew bootRun
```

#### Functionality

The processor service will:
- Store events to both PostgreSQL and S3
- Archive 1000 events at once or every 60 seconds  
- Organize files as `events/YYYY/MM/DD/HH/events-timestamp.json`
- Use AWS SDK with environment credentials
- Continue PostgreSQL processing if S3 fails

#### Verify S3 Archival

Check your S3 bucket for files like:
```
s3://your-bucket/events/2025/09/21/15/events-1758494538713.json
s3://your-bucket/events/2025/09/21/15/events-1758494540372.json
```

Each file contains 1000 events in JSON format.

## Configuration

### Database
- PostgreSQL on port 55432
- Optimized with HikariCP connection pooling
- Batch insert operations for high throughput

### Kafka
- 6 partitions for parallel processing
- Optimized consumer settings (max.poll.records: 1000)
- Session-based partitioning for data consistency

### S3 Archival
- AWS S3 integration with automatic event archiving
- Batching: 1000 events per file or 60-second rotation
- Organization: `events/YYYY/MM/DD/HH/events-timestamp.json`
- Dual-path processing: Real-time to PostgreSQL + batch to S3
- AWS SDK 2.21.29 with environment-based credentials

### Monitoring
- Health checks on /actuator/health
- Real-time metrics via Server-Sent Events
- Processing statistics and throughput monitoring
