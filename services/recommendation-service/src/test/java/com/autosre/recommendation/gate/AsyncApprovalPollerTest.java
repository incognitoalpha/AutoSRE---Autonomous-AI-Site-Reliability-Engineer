package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncApprovalPollerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RemediationProducer producer;

    @InjectMocks
    private AsyncApprovalPoller poller;

    private UUID planId1;
    private UUID planId2;
    private ApprovalGate.RemediationPlanWrapper plan1;

    @BeforeEach
    void setUp() {
        planId1 = UUID.randomUUID();
        planId2 = UUID.randomUUID();
        plan1 = new ApprovalGate.RemediationPlanWrapper(
                planId1, "agent-1", "alert-1", "service-1", "[]",
                RemediationRecommendation.RiskLevel.MEDIUM, 0.85, RemediationRecommendation.ApprovalTier.ASYNC
        );
    }

    @Test
    void testPollExpiredPlans_NoKeys() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        poller.pollExpiredPlans();

        verify(redisTemplate, never()).getExpire(anyString());
        verify(producer, never()).publishApproved(any());
    }

    @Test
    void testPollExpiredPlans_WithExpiredAndNotExpired() {
        String key1 = "autosre:pending:async:" + planId1;
        String key2 = "autosre:pending:async:" + planId2;

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(key1, key2);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        // key1 is expired
        when(redisTemplate.getExpire(key1)).thenReturn(0L);
        // key2 is not expired
        when(redisTemplate.getExpire(key2)).thenReturn(10L);

        // No veto for key1
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + planId1)).thenReturn(false);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key1)).thenReturn(plan1);

        poller.pollExpiredPlans();

        verify(producer).publishApproved(plan1);
        verify(redisTemplate).delete(key1);
        verify(redisTemplate, never()).delete(key2);
    }

    @Test
    void testPollExpiredPlans_Vetoed() {
        String key1 = "autosre:pending:async:" + planId1;

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key1);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.getExpire(key1)).thenReturn(-1L);

        // Vetoed
        when(redisTemplate.hasKey("autosre:pending:async:veto:" + planId1)).thenReturn(true);

        poller.pollExpiredPlans();

        verify(producer, never()).publishApproved(any());
        verify(redisTemplate).delete(key1);
    }

    @Test
    void testPollExpiredPlans_InvalidType() {
        String key1 = "autosre:pending:async:" + planId1;

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key1);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.getExpire(key1)).thenReturn(0L);

        when(redisTemplate.hasKey("autosre:pending:async:veto:" + planId1)).thenReturn(false);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key1)).thenReturn("Not a plan");

        poller.pollExpiredPlans();

        verify(producer, never()).publishApproved(any());
        verify(redisTemplate).delete(key1);
    }

    @Test
    void testPollExpiredPlans_ExceptionOnPublish() {
        String key1 = "autosre:pending:async:" + planId1;

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key1);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.getExpire(key1)).thenReturn(0L);

        when(redisTemplate.hasKey("autosre:pending:async:veto:" + planId1)).thenReturn(false);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key1)).thenReturn(plan1);

        doThrow(new RuntimeException("Kafka error")).when(producer).publishApproved(plan1);

        poller.pollExpiredPlans();

        verify(producer).publishApproved(plan1);
        verify(redisTemplate, never()).delete(key1); // Assuming it doesn't delete if publish fails
    }

    @Test
    void testPollExpiredPlans_InvalidUUID() {
        String keyInvalid = "autosre:pending:async:invalid-uuid";

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(keyInvalid);

        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.getExpire(keyInvalid)).thenReturn(0L);

        poller.pollExpiredPlans();

        verify(redisTemplate, never()).hasKey(anyString());
        verify(producer, never()).publishApproved(any());
    }
}
