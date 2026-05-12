package com.autosre.healing.executor;

import com.autosre.common.exception.AgentExecutionException;
import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;

/**
 * Interface for executing healing actions against Kubernetes resources.
 * Implementations handle specific action types (scale, restart, rollback).
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
public sealed interface HealingActionExecutor
        permits KubernetesScaleExecutor, PodRestartExecutor, RollbackExecutor {

    /**
     * Returns the action type this executor handles.
     *
     * @return the action type string (e.g., "SCALE_DEPLOYMENT", "RESTART_POD")
     */
    String actionType();

    /**
     * Executes the remediation action and returns the outcome.
     *
     * @param action the remediation action to execute; must not be null
     * @return the outcome of the execution including success flag and metrics
     * @throws AgentExecutionException if the execution fails after retries
     */
    HealingOutcomeEvent execute(RemediationAction.Action action) throws AgentExecutionException;
}