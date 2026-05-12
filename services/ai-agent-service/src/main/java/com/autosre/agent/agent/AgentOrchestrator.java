package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.common.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates specialist agents to handle anomaly alerts in parallel.
 * Receives root cause analysis and selects appropriate agents based on
 * affected components and root cause classification. Runs agents concurrently
 * via CompletableFuture and merges their RemediationPlan results.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Service
public class AgentOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AgentOrchestrator.class);
    private static final long AGENT_TIMEOUT_SECONDS = 30;

    private final List<SpecialistAgent> specialistAgents;

    public AgentOrchestrator(List<SpecialistAgent> specialistAgents) {
        this.specialistAgents = specialistAgents;
        LOG.info("AgentOrchestrator initialized with {} specialist agents: {}",
                specialistAgents.size(),
                specialistAgents.stream()
                        .map(SpecialistAgent::getAgentName)
                        .toList());
    }

    /**
     * Orchestrates specialist agents for the given incident context.
     * Runs applicable agents in parallel and collects their remediation plans.
     *
     * @param alertId the anomaly alert identifier
     * @param serviceId the affected service identifier
     * @param severity the severity of the anomaly
     * @param anomalyType the type of anomaly detected
     * @param anomalyDescription plain-language description of the anomaly
     * @param rootCause the identified root cause from RootCauseAgent
     * @return list of remediation plans from applicable specialist agents
     */
    public List<RemediationPlan> orchestrate(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyType,
            String anomalyDescription,
            String rootCause) {

        LOG.info("Orchestrating agents for alertId={}, service={}, type={}",
                alertId, serviceId, anomalyType);

        List<SpecialistAgent> applicableAgents = findApplicableAgents(anomalyType, rootCause);

        if (applicableAgents.isEmpty()) {
            LOG.info("No applicable agents for alertId={}", alertId);
            return List.of();
        }

        LOG.info("Selected {} applicable agents for alertId={}: {}",
                applicableAgents.size(), alertId,
                applicableAgents.stream()
                        .map(SpecialistAgent::getAgentName)
                        .toList());

        return runAgentsInParallel(alertId, serviceId, severity,
                anomalyDescription, rootCause, applicableAgents);
    }

    /**
     * Finds specialist agents applicable to the given anomaly type and root cause.
     */
    private List<SpecialistAgent> findApplicableAgents(String anomalyType, String rootCause) {
        List<SpecialistAgent> applicable = new ArrayList<>();

        for (SpecialistAgent agent : specialistAgents) {
            if (agent.isApplicable(anomalyType, rootCause)) {
                applicable.add(agent);
                LOG.debug("Agent {} is applicable", agent.getAgentName());
            }
        }

        return applicable;
    }

    /**
     * Runs applicable agents in parallel using CompletableFuture.
     */
    private List<RemediationPlan> runAgentsInParallel(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause,
            List<SpecialistAgent> agents) {

        List<CompletableFuture<RemediationPlan>> futures = new ArrayList<>();

        for (SpecialistAgent agent : agents) {
            CompletableFuture<RemediationPlan> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LOG.info("Starting agent {} for alertId={}", agent.getAgentName(), alertId);
                    RemediationPlan plan = agent.analyze(
                            alertId,
                            serviceId,
                            severity,
                            anomalyDescription,
                            rootCause
                    );
                    LOG.info("Agent {} completed for alertId={}: planId={}",
                            agent.getAgentName(), alertId, plan.planId());
                    return plan;
                } catch (Exception e) {
                    LOG.error("Agent {} failed for alertId={}: {}",
                            agent.getAgentName(), alertId, e.getMessage(), e);
                    return null;
                }
            });

            futures.add(future);
        }

        List<RemediationPlan> plans = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                CompletableFuture<RemediationPlan> future = futures.get(i);
                SpecialistAgent agent = agents.get(i);

                RemediationPlan plan = future.get(AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (plan != null) {
                    plans.add(plan);
                }
            } catch (Exception e) {
                LOG.error("Agent {} timed out or failed: {}",
                        agents.get(i).getAgentName(), e.getMessage());
            }
        }

        LOG.info("Orchestration complete for alertId={}: {} plans produced",
                alertId, plans.size());
        return plans;
    }

    /**
     * Returns the list of registered specialist agents.
     */
    public List<String> getRegisteredAgents() {
        return specialistAgents.stream()
                .map(SpecialistAgent::getAgentName)
                .toList();
    }

    /**
     * Returns the count of registered specialist agents.
     */
    public int getAgentCount() {
        return specialistAgents.size();
    }
}