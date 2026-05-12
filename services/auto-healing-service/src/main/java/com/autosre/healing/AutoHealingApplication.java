package com.autosre.healing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoSRE Auto-Healing Service - Executes remediation actions against Kubernetes.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@SpringBootApplication
public class AutoHealingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoHealingApplication.class, args);
    }
}