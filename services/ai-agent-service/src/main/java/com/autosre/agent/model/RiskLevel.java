package com.autosre.agent.model;

/**
 * Represents the risk classification for a remediation action.
 * Determines which approval gate processes the plan.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see com.autosre.recommendation.gate.ApprovalGate
 */
public enum RiskLevel {
    /** Low-risk action with high confidence — auto-execute immediately. */
    LOW,
    /** Medium-risk action with good confidence — notify on-call, auto-execute after 5 min if no veto. */
    MEDIUM,
    /** High-risk action or low confidence — block until explicit human approval. */
    HIGH
}