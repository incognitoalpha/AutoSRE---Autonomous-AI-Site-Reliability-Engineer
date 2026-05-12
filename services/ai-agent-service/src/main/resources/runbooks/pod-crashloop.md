# Pod CrashLoopBackOff Resolution Runbook

## Overview
This runbook covers the diagnosis and resolution of pods stuck in CrashLoopBackOff state,
which indicates repeated container failures and restarts.

## Symptoms
- Pod status shows `CrashLoopBackOff` or repeated restarts
- `kubectl describe pod` shows `Back-off restarting failed container`
- Application logs show repeated startup failures
- Increased latency or errors for affected service

## Common Causes

### 1. Application Errors
- Uncaught exceptions during startup
- Missing configuration or environment variables
- Failed database migrations
- Port already in use

### 2. Resource Issues
- Out of memory (OOMKilled)
- CPU throttling
- Insufficient storage space

### 3. Dependency Issues
- Database connection failures
- External API timeouts
- Configuration service unavailable

## Diagnosis Steps

### 1. Check Pod Status and Events
```bash
kubectl describe pod <pod-name> -n <namespace>

# Check recent events
kubectl get events --sort-by=.lastTimestamp -n <namespace> | tail -20
```

### 2. Review Container Logs
```bash
# View logs from previous (crashed) container
kubectl logs <pod-name> -n <namespace> --previous

# View all container logs
kubectl logs <pod-name> -n <namespace> --all-containers
```

### 3. Check Resource Limits
```bash
kubectl top pod <pod-name> -n <namespace>
kubectl describe pod <pod-name> -n <namespace> | grep -A5 "Limits"
```

### 4. Verify Dependencies
```bash
# Check if database is reachable
kubectl exec -it <pod-name> -n <namespace> -- curl -s database:5432

# Check DNS resolution
kubectl exec -it <pod-name> -n <namespace> -- nslookup service-name
```

## Resolution Steps

### For OOM Issues (Auto-Executable)
1. **Identify high memory usage pod**
2. **Increase memory limits** via deployment patch
3. **Restart pod** to apply new limits

### For Application Errors
1. **Review application logs** for startup errors
2. **Check environment variables** are correctly set
3. **Verify init containers** completed successfully
4. **Roll back to previous version** if recent deployment caused issue

### For Dependency Issues
1. **Check dependent services** are healthy
2. **Review connection strings** and credentials
3. **Add retry logic** for transient failures

## Quick Fix Commands

```bash
# Delete pod to trigger fresh restart (if issue is transient)
kubectl delete pod <pod-name> -n <namespace>

# Scale down and up to force fresh deployment
kubectl scale deployment <deployment> --replicas=0 -n <namespace>
kubectl scale deployment <deployment> --replicas=3 -n <namespace>

# Port-forward to debug locally
kubectl port-forward <pod-name> 8080:8080 -n <namespace>
```

## Prevention

1. Set appropriate resource requests and limits
2. Add startup probes for slow-starting applications
3. Implement graceful shutdown handlers
4. Add health checks (`/health`, `/ready`)
5. Monitor restart count metric

## Metrics to Monitor
- `pod_restart_count`
- `container_memory_working_set_bytes`
- `pod_startup_duration_seconds`
- `liveness_probe_failures_total`

## Related Runbooks
- High Latency (downstream dependency): `high-latency-downstream.md`
- JVM Memory Issues: `kafka-memory-saturation.md`