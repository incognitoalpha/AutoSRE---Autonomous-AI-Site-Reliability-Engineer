package com.autosre.agent.agent;

import com.autosre.agent.model.RootCauseAnalysis;

/**
 * Represents the result of a root cause analysis operation.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @param rca the root cause analysis result
 * @param success whether the analysis completed successfully
 * @param errorMessage error message if analysis failed
 */
public record RootCauseAnalysisResult(
        RootCauseAnalysis rca,
        boolean success,
        String errorMessage
) {

    /**
     * Creates a successful result.
     *
     * @param rca the analysis result
     * @return a successful result record
     */
    public static RootCauseAnalysisResult success(RootCauseAnalysis rca) {
        return new RootCauseAnalysisResult(rca, true, null);
    }

    /**
     * Creates a failure result.
     *
     * @param errorMessage description of what went wrong
     * @return a failure result record
     */
    public static RootCauseAnalysisResult failure(String errorMessage) {
        return new RootCauseAnalysisResult(null, false, errorMessage);
    }

    /**
     * Creates a failure result from an exception.
     *
     * @param cause the exception that caused the failure
     * @return a failure result record
     */
    public static RootCauseAnalysisResult failure(Throwable cause) {
        return new RootCauseAnalysisResult(null, false, cause.getMessage());
    }
}