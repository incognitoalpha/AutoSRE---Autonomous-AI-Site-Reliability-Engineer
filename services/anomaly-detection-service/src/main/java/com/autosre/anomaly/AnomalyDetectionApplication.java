package com.autosre.anomaly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Anomaly Detection Service.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class AnomalyDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnomalyDetectionApplication.class, args);
    }
}