package com.autosre.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link VectorStoreConfig}.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
class VectorStoreConfigTest {

    private VectorStoreConfig vectorStoreConfig;

    @BeforeEach
    void setUp() {
        vectorStoreConfig = new VectorStoreConfig();
        ReflectionTestUtils.setField(vectorStoreConfig, "datasourceUrl", "jdbc:postgresql://localhost:5432/autosre");
        ReflectionTestUtils.setField(vectorStoreConfig, "username", "autosre");
        ReflectionTestUtils.setField(vectorStoreConfig, "password", "autosre");
    }

    @Test
    @DisplayName("Should parse host from JDBC URL correctly")
    void parseHostExtractsCorrectly() {
        String host = vectorStoreConfig.parseHost("jdbc:postgresql://myhost.example.com:5432/mydb");
        assertThat(host).isEqualTo("myhost.example.com");
    }

    @Test
    @DisplayName("Should parse port from JDBC URL correctly")
    void parsePortExtractsCorrectly() {
        int port = vectorStoreConfig.parsePort("jdbc:postgresql://localhost:5432/autosre");
        assertThat(port).isEqualTo(5432);
    }

    @Test
    @DisplayName("Should parse database name from JDBC URL correctly")
    void parseDatabaseExtractsCorrectly() {
        String db = vectorStoreConfig.parseDatabase("jdbc:postgresql://localhost:5432/autosre");
        assertThat(db).isEqualTo("autosre");
    }

    @Test
    @DisplayName("Should handle custom port in JDBC URL")
    void parsePortHandlesCustomPort() {
        int port = vectorStoreConfig.parsePort("jdbc:postgresql://localhost:5433/autosre");
        assertThat(port).isEqualTo(5433);
    }

    @Test
    @DisplayName("Should handle JDBC URL with query parameters")
    void parseDatabaseHandlesQueryParams() {
        String db = vectorStoreConfig.parseDatabase("jdbc:postgresql://localhost:5432/autosre?ssl=true");
        assertThat(db).isEqualTo("autosre");
    }
}