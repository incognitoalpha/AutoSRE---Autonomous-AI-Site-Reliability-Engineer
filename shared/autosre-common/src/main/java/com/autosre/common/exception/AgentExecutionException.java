package com.autosre.common.exception;

/**
 * Exception thrown when agent execution fails (e.g., LLM call, tool invocation).
 *
 * <p>Bounded context: {@code autosre-common}</p>
 */
public class AgentExecutionException extends AutoSreException {

    private final String agentId;
    private final String context;

    public AgentExecutionException(String message) {
        super("AGENT_EXECUTION_ERROR", message);
        this.agentId = "unknown";
        this.context = null;
    }

    public AgentExecutionException(String agentId, String message) {
        super("AGENT_EXECUTION_ERROR", message);
        this.agentId = agentId;
        this.context = null;
    }

    public AgentExecutionException(String agentId, String message, String context, Throwable cause) {
        super("AGENT_EXECUTION_ERROR", message, cause);
        this.agentId = agentId;
        this.context = context;
    }

    public AgentExecutionException(String agentId, String message, Throwable cause) {
        super("AGENT_EXECUTION_ERROR", message, cause);
        this.agentId = agentId;
        this.context = null;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getContext() {
        return context;
    }
}