package com.autosre.agent.rag;

import java.util.List;

/**
 * Service for retrieving relevant runbook chunks from pgvector based on semantic similarity.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
public interface RunbookRetrievalService {

    /**
     * Retrieves the top-N most relevant runbook chunks for a given query.
     *
     * @param query the natural language query describing the incident
     * @param limit maximum number of runbook chunks to return (default 3)
     * @return list of relevant runbook chunks with their titles and scores
     */
    List<RunbookChunk> retrieveRelevantRunbooks(String query, int limit);

    /**
     * Retrieves the top-3 most relevant runbook chunks.
     *
     * @param query the natural language query describing the incident
     * @return list of top-3 relevant runbook chunks
     */
    default List<RunbookChunk> retrieveRelevantRunbooks(String query) {
        return retrieveRelevantRunbooks(query, 3);
    }

    /**
     * Represents a chunk of a runbook retrieved from pgvector.
     *
     * @param title the runbook title
     * @param chunk the text content of this chunk
     * @param similarityScore the vector similarity score (0.0 to 1.0)
     * @param section the section/heading this chunk belongs to
     */
    record RunbookChunk(
            String title,
            String chunk,
            double similarityScore,
            String section
    ) {

        /**
         * Validates the chunk data.
         */
        public RunbookChunk {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title must not be blank");
            }
            if (chunk == null || chunk.isBlank()) {
                throw new IllegalArgumentException("chunk must not be blank");
            }
            if (similarityScore < 0.0 || similarityScore > 1.0) {
                throw new IllegalArgumentException(
                        "similarityScore must be between 0.0 and 1.0");
            }
        }

        /**
         * Formats this chunk for inclusion in a prompt.
         *
         * @return formatted string suitable for prompt injection
         */
        public String toPromptFragment() {
            return String.format(
                    """
                    Runbook: %s
                    Section: %s
                    Content: %s
                    Relevance: %.2f
                    """,
                    title, section != null ? section : "General", chunk, similarityScore
            );
        }
    }
}