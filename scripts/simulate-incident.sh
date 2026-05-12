#!/bin/bash
# simulate-incident.sh - Injects synthetic telemetry events into Kafka for testing

set -e

KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
TOPIC_METRICS="autosre.telemetry.metrics"
TOPIC_LOGS="autosre.telemetry.logs"

echo "=== AutoSRE Incident Simulation ==="
echo "Bootstrap: $KAFKA_BOOTSTRAP"

# Check if Kafka is accessible
if ! kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --list > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to Kafka at $KAFKA_BOOTSTRAP"
    echo "Ensure Kafka is running: docker compose up -d kafka"
    exit 1
fi

echo "Kafka connection OK"
echo ""

# Function to send a metric event
send_metric() {
    local service=$1
    local metric_name=$2
    local value=$3
    local timestamp=$(date +%s000)

    cat << EOF | kafka-console-producer.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --topic "$TOPIC_METRICS"
{"serviceId":"$service","metricName":"$metric_name","value":$value,"timestamp":$timestamp,"labels":{"env":"test","region":"us-east-1"}}
EOF
}

# Function to send a log event
send_log() {
    local service=$1
    local level=$2
    local message=$3

    cat << EOF | kafka-console-producer.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --topic "$TOPIC_LOGS"
{"serviceId":"$service","level":"$level","message":"$message","timestamp":$(date -u +%Y-%m-%dT%H:%M:%SZ),"traceId":"$(uuidgen)"}
EOF
}

echo "=== Scenario 1: High Latency Spike ==="
echo "Injecting 50 high-latency metric events for payment-service..."

for i in {1..50}; do
    send_metric "payment-service" "http_request_latency_ms" $((150 + RANDOM % 100))
    send_metric "payment-service" "error_rate" "0.05"
    sleep 0.1
done

echo ""
echo "=== Scenario 2: Memory Saturation ==="
echo "Injecting memory saturation metrics for order-service..."

for i in {1..30}; do
    send_metric "order-service" "jvm_heap_usage_percent" $((85 + RANDOM % 10))
    send_metric "order-service" "gc_pause_ms" $((100 + RANDOM % 200))
    sleep 0.1
done

echo ""
echo "=== Scenario 3: Error Rate Spike ==="
echo "Injecting error logs for inventory-service..."

for i in {1..20}; do
    send_log "inventory-service" "ERROR" "Connection pool exhausted - unable to acquire DB connection"
    send_log "inventory-service" "WARN" "Slow query detected: execution time exceeded 5000ms threshold"
    sleep 0.2
done

echo ""
echo "=== Scenario 4: Throughput Drop ==="
echo "Injecting low throughput metrics for user-service..."

for i in {1..25}; do
    send_metric "user-service" "requests_per_second" $((5 + RANDOM % 10))
    send_metric "user-service" "active_connections" $((2 + RANDOM % 3))
    sleep 0.1
done

echo ""
echo "=== Simulation Complete ==="
echo "Check anomaly-detection-service logs to see alerts generated"
echo "Verify with: docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic autosre.alerts.anomalies --from-beginning"