# Kafka Memory Saturation Runbook

## Overview
This runbook covers the diagnosis and resolution of Kafka broker memory saturation issues,
which can lead to OOM (Out of Memory) kills, broker unavailability, and consumer lag spikes.

## Symptoms
- Broker pod in CrashLoopBackOff or OOMKilled state
- Memory utilization > 85% sustained for > 5 minutes
- Consumer lag increasing rapidly
- Produce/consume operations timing out
- Logs showing "memory allocation failed" or similar OOM errors

## Diagnosis Steps

### 1. Check Current Memory Utilization
```bash
# Check pod memory usage
kubectl top pods -n kafka | grep broker

# Check broker JMX metrics
curl -s broker:8080/jmx?qry=kafka.server:type=KafkaServerBroker,name=MemoryStats
```

### 2. Identify Memory Pressure Source
- Check heap utilization: `kafka-topics.sh --describe --under-replicated-partitions`
- Review consumer group lag: `kafka-consumer-groups.sh --describe --group <group>`
- Check for excessive retained messages in producer buffers

### 3. Common Causes
1. **Large message batches**: Producers sending messages > 1MB each
2. **Slow consumers**: Consumer lag causing message retention
3. **Partition imbalance**: Some brokers handling more traffic than others
4. **Memory leak**: Off-heap memory growing unbounded (NIO buffers, etc.)

## Resolution Steps

### Immediate Actions (Auto-Execute for LOW/MEDIUM Risk)

1. **Scale Kafka Brokers** (if under-replicated partitions exist)
   ```bash
   kubectl scale statefulset kafka --replicas=N+1 -n kafka
   ```

2. **Restart Broker Pod** (to clear memory leaks)
   ```bash
   kubectl delete pod kafka-broker-X -n kafka
   ```

### High Risk Actions (Require Sync Approval)

1. **Increase Memory Limits**
   - Edit the Kafka StatefulSet
   - Increase `memory.limit` in container spec
   - Rolling restart required

2. **Adjust JVM Heap Settings**
   ```yaml
   env:
     - name: KAFKA_HEAP_OPTS
       value: "-Xms4g -Xmx4g -XX:+UseG1GC"
   ```

## Prevention

1. Monitor memory utilization at 70% threshold
2. Set up alerts for sustained high memory
3. Implement producer rate limiting
4. Regular partition rebalancing

## Related Metrics
- `kafka_server_BrokerTopicManager_MeanConsumerLag`
- `kafka_server_KafkaRequestHandlerPool_meanRequestQueueTimeMs`
- `memory_usage_percent`
- `oom_kills_total`

## Post-Incident Actions
1. Analyze heap dumps if memory leak suspected
2. Review producer configurations for batch size limits
3. Update monitoring thresholds
4. Document any new producers that may have caused the issue