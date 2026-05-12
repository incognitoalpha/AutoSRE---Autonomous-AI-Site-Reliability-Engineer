package com.autosre.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * Configures REST controllers and WebSocket endpoints.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Configuration
@EnableWebFlux
public class WebConfig {

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}