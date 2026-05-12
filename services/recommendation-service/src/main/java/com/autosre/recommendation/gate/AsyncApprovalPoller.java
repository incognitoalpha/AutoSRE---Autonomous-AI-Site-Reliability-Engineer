package com.autosre.recommendation.gate;

import static com.autosre.recommendation.gate.ApprovalGate.RemediationPlanWrapper;

import com.autosre.recommendation.producer.RemediationProducer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that scans Redis for async approval plans that have
 * exceeded their TTL and publishes them to the remediation topic.
 * Uses SCAN instead of KEYS to avoid blocking production Redis instances.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public final class AsyncApprovalPoller {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncApprovalPoller.class);
    private static final String REDIS_KEY_PREFIX = "autosre:pending:async:";
    private static final int SCAN_COUNT = 100;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RemediationProducer producer;

    public AsyncApprovalPoller(
            RedisTemplate<String, Object> redisTemplate,
            RemediationProducer producer) {
        this.redisTemplate = redisTemplate;
        this.producer = producer;
    }

    /**
     * Scans for expired pending plans every 30 seconds.
     * Uses SCAN with COUNT=100 to avoid O(N) Redis blocking.
     * A plan is considered expired when its remaining TTL is zero or negative.
     */
    @Scheduled(fixedDelayString = "${autosre.async.poll-interval-ms:30000}")
    public void pollExpiredPlans() {
        LOG.debug("Scanning for expired async approval plans");

        ScanOptions options = ScanOptions.scanOptions()
                .match(REDIS_KEY_PREFIX + "*")
                .count(SCAN_COUNT)
                .build();

        List<String> expiredKeys = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (key.contains(":veto:")) {
                    continue;
                }
                try {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl <= 0) {
                        expiredKeys.add(key);
                    }
                } catch (Exception e) {
                    LOG.error("Error checking key {}: {}", key, e.getMessage());
                }
            }
        }

        for (String key : expiredKeys) {
            UUID planId = extractPlanId(key);
            if (planId != null) {
                publishIfNotVetoed(planId, key);
            }
        }

        if (!expiredKeys.isEmpty()) {
            LOG.info("Found {} expired async plans to process", expiredKeys.size());
        }
    }

    private UUID extractPlanId(String key) {
        String planIdStr = key.replace(REDIS_KEY_PREFIX, "");
        try {
            return UUID.fromString(planIdStr);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid plan ID in key: {}", key);
            return null;
        }
    }

    private void publishIfNotVetoed(UUID planId, String planKey) {
        String vetoKey = REDIS_KEY_PREFIX + "veto:" + planId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(vetoKey))) {
            LOG.info("Plan {} was vetoed, not publishing", planId);
            redisTemplate.delete(planKey);
            return;
        }

        Object value = redisTemplate.opsForValue().get(planKey);
        if (!(value instanceof RemediationPlanWrapper)) {
            LOG.warn("Plan {} not found or invalid type, skipping", planId);
            redisTemplate.delete(planKey);
            return;
        }

        RemediationPlanWrapper plan = (RemediationPlanWrapper) value;
        try {
            producer.publishApproved(plan);
            redisTemplate.delete(planKey);
            LOG.info("Published expired async plan: planId={}", planId);
        } catch (Exception e) {
            LOG.error("Failed to publish expired plan {}: {}", planId, e.getMessage(), e);
        }
    }
}
