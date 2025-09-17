# Real-time Analytics (Processor + Ingest + Infra)

This repo contains:

- `processor-service` — Spring Boot service that consumes Kafka `events`, writes to Postgres, and serves SSE metrics at `/metrics/stream`.
- `ingest-service` — Spring Boot service that accepts event POSTs and publishes to Kafka `events`.
- `infra` — Docker Compose for Postgres, Kafka (with KRaft). Optional override adds Kafka Connect + Kafka UI.
- `dashboard` — (optional) Vite/React dashboard that streams metrics and sends test events.

## Quick start

### 1) Infra

```bash
docker compose -f infra/docker-compose.yml up -d
# Optional (Connect + Kafka UI):
docker compose -f infra/docker-compose.yml -f infra/docker-compose.override.yml --profile connect up -d
# Create topic once:
docker compose -f infra/docker-compose.yml exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic events --partitions 6 --replication-factor 1 || true
```

### 2) Services

```bash
cd processor-service && ./gradlew bootRun
# in another terminal
cd ingest-service && ./gradlew bootRun
```

### 3) Test

- SSE: `curl -N http://localhost:8082/metrics/stream`
- Send event:

```bash
curl -X POST http://localhost:8081/events \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"s-1","eventType":"page_view","payload":"{\"path\":\"/\"}"}'
```

### 4) Dashboard (optional)

```bash
cd dashboard
cp .env.example .env
npm install
npm run dev
# open http://localhost:5173
```

### 5) Kafka Connect → S3 (optional)

- Put AWS keys in `infra/.env` (not committed).
- Start with override profile and POST the connector JSON in `infra/connectors/`.
