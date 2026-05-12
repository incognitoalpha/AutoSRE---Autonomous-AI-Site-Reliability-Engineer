package com.autosre.healing.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Fabric8 Kubernetes client for cluster operations.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 *
 * @param masterUrl   the Kubernetes API server URL; defaults to in-cluster URL
 * @param namespace   the target namespace; defaults to current context namespace
 * @param caCertPath  optional path to CA certificate for TLS verification
 * @param token       optional service account token for authentication
 */
@Configuration
public class KubernetesConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesConfig.class);

    @Value("${autosre.kubernetes.master-url:}")
    private String masterUrl;

    @Value("${autosre.kubernetes.namespace:default}")
    private String namespace;

    @Value("${autosre.kubernetes.ca-cert-path:}")
    private String caCertPath;

    @Value("${autosre.kubernetes.token:}")
    private String token;

    @Value("${autosre.kubernetes.in-cluster:true}")
    private boolean inCluster;

    @Bean(destroyMethod = "close")
    public KubernetesClient kubernetesClient() {
        LOG.info("Initializing Kubernetes client: inCluster={}, namespace={}", inCluster, namespace);

        ConfigBuilder builder = new ConfigBuilder();

        if (!inCluster) {
            builder.withMasterUrl(masterUrl.isEmpty() ? "http://localhost:8443" : masterUrl);
            LOG.info("Running in local mode - connecting to: {}", masterUrl);
        }

        if (!caCertPath.isEmpty()) {
            builder.withCaCertFile(caCertPath);
        }

        if (!token.isEmpty()) {
            builder.withOauthToken(token);
        }

        builder.withNamespace(namespace);
        builder.withRequestTimeout(30_000);
        builder.withConnectionTimeout(10_000);

        Config config = builder.build();
        return new DefaultKubernetesClient(config);
    }
}