package com.autosre.gateway.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * WebSocket configuration for reactive socket handling.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}