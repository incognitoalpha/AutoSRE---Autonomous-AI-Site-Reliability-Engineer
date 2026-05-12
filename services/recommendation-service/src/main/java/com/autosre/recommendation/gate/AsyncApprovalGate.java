package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import java.util.UUID;

/**
 * Approval gate for medium-risk remediation plans.
 * Stores plan in Redis with 5-minute TTL.
 * Auto-publishes after TTL unless vetoed.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public final class AsyncApprovalGate implements ApprovalGate {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncApprovalGate.class);
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String REDIS_KEY_PREFIX = "autosre:pending:async:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RemediationProducer producer;
    private final AsyncVetoChecker vetoChecker;

    public AsyncApprovalGate(
            RedisTemplate<String, Object> redisTemplate,
            RemediationProducer producer,
            AsyncVetoChecker vetoChecker) {
        this.redisTemplate = redisTemplate;
        this.producer = producer;
        this.vetoChecker = vetoChecker;
    }

    @Override
    public boolean supports(RemediationRecommendation.RiskLevel riskLevel) {
        return riskLevel == RemediationRecommendation.RiskLevel.MEDIUM;
    }

    @Override
    public ApprovalDecision process(RemediationPlanWrapper plan) {
        LOG.info("AsyncApprovalGate processing plan: planId={}, confidence={}, TTL={}",
                plan.planId(), plan.confidenceScore(), TTL);

        String key = REDIS_KEY_PREFIX + plan.planId();

        try {
            redisTemplate.opsForValue().set(key, plan, TTL);
            LOG.info("Plan {} stored in Redis with {} TTL", plan.planId(), TTL);

            return ApprovalDecision.PENDING;

        } catch (Exception e) {
            LOG.error("Failed to store plan {} in Redis: {}", plan.planId(), e.getMessage(), e);
            return ApprovalDecision.REJECTED;
        }
    }

    /**
     * Checks if a veto has been registered for this plan.
     */
    public boolean isVetoed(UUID planId) {
        String key = REDIS_KEY_PREFIX + "veto:" + planId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Registers a veto for a pending plan.
     */
    public void registerVeto(UUID planId, String reason) {
        String key = REDIS_KEY_PREFIX + "veto:" + planId;
        redisTemplate.opsForValue().set(key, reason, Duration.ofHours(1));
        LOG.info("Veto registered for plan {}: {}", planId, reason);
    }

    /**
     * Retrieves the pending plan from Redis.
     */
    public RemediationPlanWrapper getPendingPlan(UUID planId) {
        String key = REDIS_KEY_PREFIX + planId;
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof RemediationPlanWrapper ? (RemediationPlanWrapper) value : null;
    }

    /**
     * Publishes a plan if not vetoed and TTL expired.
     */
    public void checkAndPublish(UUID planId) {
        if (isVetoed(planId)) {
            LOG.info("Plan {} was vetoed, not publishing", planId);
            return;
        }

        RemediationPlanWrapper plan = getPendingPlan(planId);
        if (plan == null) {
            LOG.warn("Plan {} not found in Redis, may have expired", planId);
            return;
        }

        try {
            producer.publishApproved(plan);
            redisTemplate.delete(REDIS_KEY_PREFIX + planId);
            LOG.info("Plan {} published after async wait", planId);
        } catch (Exception e) {
            LOG.error("Failed to publish plan {}: {}", planId, e.getMessage(), e);
        }
    }
}