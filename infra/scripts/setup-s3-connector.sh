#!/bin/bash

# Setup S3 Sink Connector for Kafka
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Setting up S3 Sink Connector...${NC}"

# Check if Kafka Connect is running
if ! curl -s http://localhost:8083/ > /dev/null 2>&1; then
    echo -e "${RED}Error: Kafka Connect is not running on port 8083${NC}"
    echo "Please start with: docker compose -f infra/docker-compose.yml up -d kafka-connect"
    exit 1
fi

# Check environment variables
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo -e "${RED}Error: AWS credentials not set${NC}"
    echo "Please set environment variables:"
    echo "export AWS_ACCESS_KEY_ID=your_access_key"
    echo "export AWS_SECRET_ACCESS_KEY=your_secret_key"
    exit 1
fi

# Check if bucket name is configured
if grep -q "YOUR_BUCKET_NAME" infra/connectors/s3-sink.json; then
    echo -e "${RED}Error: S3 bucket name not configured${NC}"
    echo "Please update 's3.bucket.name' in infra/connectors/s3-sink.json"
    exit 1
fi

# Wait for Kafka Connect to be ready
echo "Waiting for Kafka Connect to be ready..."
until curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
    sleep 2
done

# Deploy the S3 sink connector
echo "Deploying S3 sink connector..."
curl -X POST http://localhost:8083/connectors \
    -H 'Content-Type: application/json' \
    -d @infra/connectors/s3-sink.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ S3 sink connector deployed successfully!${NC}"
    echo
    echo "To check status:"
    echo "curl http://localhost:8083/connectors/s3-sink-events/status"
    echo
    echo "To view all connectors:"
    echo "curl http://localhost:8083/connectors"
else
    echo -e "${RED}✗ Failed to deploy S3 sink connector${NC}"
    exit 1
fi