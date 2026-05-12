package com.autosre.agent.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the pgvector-backed embedding store for RAG operations.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see <a href="https://github.com/langchain4j/langchain4j">LangChain4j pgvector module</a>
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * Creates the pgvector embedding store pointing to the incidents table.
     * The table must have a column of type {@code vector(1536)} for embeddings.
     *
     * @param embeddingModel the embedding model used to generate vectors
     * @return configured {@link PgVectorEmbeddingStore} instance
     */
    @Bean
    public PgVectorEmbeddingStore embeddingStore(EmbeddingModel embeddingModel) {
        // Parse jdbc:postgresql://host:port/database format
        String host = parseHost(datasourceUrl);
        int port = parsePort(datasourceUrl);
        String database = parseDatabase(datasourceUrl);

        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .user(username)
                .password(password)
                .database(database)
                .table("incidents")
                .dimension(1536)
                .build();
    }

    /**
     * Extracts the host from a JDBC URL.
     *
     * @param jdbcUrl the JDBC URL to parse
     * @return the host component or "localhost" if not found
     */
    String parseHost(String jdbcUrl) {
        // jdbc:postgresql://host:port/database
        String between = jdbcUrl.replace("jdbc:postgresql://", "");
        int colon = between.indexOf(':');
        return colon > 0 ? between.substring(0, colon) : "localhost";
    }

    int parsePort(String jdbcUrl) {
        String between = jdbcUrl.replace("jdbc:postgresql://", "");
        int colon = between.indexOf(':');
        int slash = between.indexOf('/');
        if (colon > 0 && slash > colon) {
            return Integer.parseInt(between.substring(colon + 1, slash));
        }
        return 5432;
    }

    String parseDatabase(String jdbcUrl) {
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?');
        if (lastSlash > 0) {
            int end = questionMark > lastSlash ? questionMark : jdbcUrl.length();
            return jdbcUrl.substring(lastSlash + 1, end);
        }
        return "autosre";
    }
}