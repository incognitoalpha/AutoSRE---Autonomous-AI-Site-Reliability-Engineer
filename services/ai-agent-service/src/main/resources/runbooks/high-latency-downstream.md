# High Latency Downstream Dependency Runbook

## Overview
This runbook covers diagnosis and resolution when a service experiences high latency due to
slow downstream dependencies (databases, external APIs, other microservices).

## Symptoms
- P99 latency > 500ms (service-specific threshold)
- Error rate increase (timeouts on downstream calls)
- Circuit breaker open events
- Increase in `http_server_requests_seconds` for affected endpoints
- User-reported slow response times

## Diagnosis Steps

### 1. Identify Slow Endpoint
```bash
# Check application metrics
curl -s prometheus:9090/api/v1/query?query=http_server_requests_seconds_quantile

# Find slowest endpoints
kubectl exec -it <pod> -- curl localhost:8080/actuator/metrics/http.server.requests
```

### 2. Check Downstream Dependencies
```bash
# Database latency
kubectl exec -it <pod> -- psql -c "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 5;"

# External API latency (check application logs)
kubectl logs <pod> | grep "downstream" | tail -50
```

### 3. Correlate with Infrastructure
- Check if database CPU/memory is high
- Verify network latency between services
- Review recent deployments (downstream or own service)

## Common Causes

### 1. Database Issues
- Missing indexes on slow queries
- Connection pool exhaustion
- Large table scans
- Lock contention

### 2. External API Issues
- API rate limiting
- Network partition
- Partner service degradation
- Timeout misconfiguration

### 3. Internal Service Issues
- Recent deployment introducing performance regression
- Resource exhaustion (CPU, memory, connections)
- Thread pool饱和

## Resolution Steps

### For Database Issues (Auto-Executable for LOW Risk)
1. **Scale connection pool** (if at limit)
2. **Add missing indexes** via migration
3. **Clear connection pool** (restart app if needed)

### For External API Issues
1. **Check partner status pages**
2. **Enable circuit breaker** if not already
3. **Implement retry with backoff** for transient failures
4. **Fall back to cached data** if applicable

### For Internal Issues
1. **Roll back recent deployment** if regression detected
2. **Increase resources** (CPU/memory limits)
3. **Enable query caching** for frequently-accessed data

## Database-Specific Fixes

### PostgreSQL Slow Query
```sql
-- Find slow queries
SELECT * FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC;

-- Add index
CREATE INDEX CONCURRENTLY idx_table_column ON table_name(column);

-- Force plan (if index not used)
SET enable_seqscan = off;
```

### Connection Pool Exhaustion
```yaml
# Spring Boot application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

## Metrics to Monitor
- `db_query_duration_seconds`
- `external_api_latency_seconds`
- `connection_pool_active_connections`
- `circuit_breaker_state`
- `cache_hit_ratio`

## Prevention

1. Implement circuit breakers (Resilience4j)
2. Add request timeouts for all downstream calls
3. Monitor p95/p99 latency, not just averages
4. Regular database EXPLAIN ANALYZE
5. Implement caching for expensive queries
6. Load test before major releases

## Related Runbooks
- Pod CrashLoopBackOff: `pod-crashloop.md`
- Kafka Memory Saturation: `kafka-memory-saturation.md`