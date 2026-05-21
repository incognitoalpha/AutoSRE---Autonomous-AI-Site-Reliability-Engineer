package com.autosre.recommendation.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfidenceScoringServiceTest {

    private ConfidenceScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ConfidenceScoringService();
    }

    @Test
    void testComputeScore_Default() {
        double score = scoringService.computeScore(0.5, "unknown", false, false, 0);
        assertEquals(0.5, score, 0.001);
    }

    @Test
    void testComputeScore_NullRootCause() {
        double score = scoringService.computeScore(0.5, null, false, false, 0);
        assertEquals(0.5, score, 0.001);
    }

    @Test
    void testComputeScore_RunbookMatch() {
        double score = scoringService.computeScore(0.5, "unknown", true, false, 0);
        assertEquals(0.65, score, 0.001); // 0.5 + 0.15
    }

    @Test
    void testComputeScore_LowResolutionRate() {
        scoringService.recordResolution("network_issue", false);
        scoringService.recordResolution("network_issue", false);
        scoringService.recordResolution("network_issue", false);
        scoringService.recordResolution("network_issue", false);
        
        double score = scoringService.computeScore(0.8, "network_issue", false, false, 0);
        // Current implementation penalized by 0.20 if resolutionRate < 0.8
        assertEquals(0.6, score, 0.001); 
    }

    @Test
    void testComputeScore_SimilarIncidents() {
        double score = scoringService.computeScore(0.5, "unknown", false, false, 2);
        assertEquals(0.6, score, 0.001); // 0.5 + 0.10
    }

    @Test
    void testComputeScore_CriticalSeverity() {
        double score = scoringService.computeScore(0.5, "unknown", false, true, 0);
        assertEquals(0.55, score, 0.001); // 0.5 + 0.05
    }

    @Test
    void testComputeScore_BoundedToMax() {
        double score = scoringService.computeScore(0.9, "unknown", true, true, 5);
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void testComputeScore_BoundedToMin() {
        double score = scoringService.computeScore(0.0, "network_issue", false, false, 0);
        // network_issue starts at 0.70 < 0.8 -> penalty of 0.2
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testRecordResolution() {
        scoringService.recordResolution("new_issue", true);
        assertEquals(1.0, scoringService.getResolutionRate("new_issue"), 0.001);

        scoringService.recordResolution("new_issue", false);
        assertEquals(0.8, scoringService.getResolutionRate("new_issue"), 0.001); // 1.0*0.8 + 0.0
    }

    @Test
    void testGetResolutionRate_Unknown() {
        assertEquals(0.5, scoringService.getResolutionRate("unknown_issue"), 0.001);
    }
}
