package com.autosre.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoSRE API Gateway Service - REST API and WebSocket for dashboard.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}