package com.autosre.agent.rag;

import com.autosre.agent.model.RootCauseAnalysis;
import java.util.List;

/**
 * Service for storing and retrieving historical incident data with embeddings.
 * Provides long-term memory for the AI agent using PostgreSQL with pgvector.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RootCauseAnalysis
 */
public interface IncidentMemoryService {

    /**
     * Stores a root cause analysis as a new incident in the database.
     * Generates an embedding and stores it alongside the incident data.
     *
     * @param rca the root cause analysis to store
     */
    void store(RootCauseAnalysis rca);

    /**
     * Finds the most similar historical incidents to the given query.
     *
     * @param query natural language query describing the incident
     * @param limit maximum number of results to return
     * @return list of similar incidents with their similarity scores
     */
    List<HistoricalIncidentMatch> findSimilar(String query, int limit);

    /**
     * Finds the 5 most similar historical incidents.
     *
     * @param query natural language query describing the incident
     * @return list of similar incidents with similarity scores
     */
    default List<HistoricalIncidentMatch> findSimilar(String query) {
        return findSimilar(query, 5);
    }

    /**
     * Finds incidents by service identifier.
     *
     * @param serviceId the service identifier to search for
     * @param limit maximum number of results to return
     * @return list of incidents for the given service
     */
    List<HistoricalIncidentMatch> findByServiceId(String serviceId, int limit);

    /**
     * Represents a historical incident retrieved from memory.
     *
     * @param incidentId unique identifier of the incident
     * @param serviceId the affected service
     * @param rootCause the root cause analysis text
     * @param recommendedActions the recommended remediation actions
     * @param similarityScore vector similarity score (0.0 to 1.0)
     * @param detectedAt when the incident was detected
     */
    record HistoricalIncidentMatch(
            String incidentId,
            String serviceId,
            String rootCause,
            List<String> recommendedActions,
            double similarityScore,
            String detectedAt
    ) {

        /**
         * Validates that similarity score is within valid range.
         */
        public HistoricalIncidentMatch {
            if (similarityScore < 0.0 || similarityScore > 1.0) {
                throw new IllegalArgumentException(
                        "similarityScore must be between 0.0 and 1.0");
            }
        }
    }
}