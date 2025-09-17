#!/usr/bin/env bash
set -euo pipefail

# Run from infra/ (the tasks already set cwd to infra)
echo "Waiting for Kafka to be ready..."
for i in {1..60}; do
  if docker compose exec -T kafka bash -lc "/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list" >/dev/null 2>&1; then
    echo "Kafka is up."
    break
  fi
  sleep 2
  if [[ $i -eq 60 ]]; then
    echo "Kafka did not become ready in time." >&2
    exit 1
  fi
done

echo "Creating topics (idempotent)..."
docker compose exec -T kafka bash -lc "/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --topic events --partitions 6 --replication-factor 1 --bootstrap-server localhost:9092"
docker compose exec -T kafka bash -lc "/opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists --topic events-dlq --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092"

echo "Topics created."