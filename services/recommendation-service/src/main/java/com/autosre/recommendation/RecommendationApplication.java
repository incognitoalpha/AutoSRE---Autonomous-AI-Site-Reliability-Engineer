package com.autosre.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application for the recommendation-service.
 * Handles confidence scoring, approval gate routing, and publishing
 * of approved remediation plans to Kafka.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@SpringBootApplication
@EnableScheduling
public class RecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}