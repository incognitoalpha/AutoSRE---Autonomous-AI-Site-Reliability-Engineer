package com.autosre.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler that bridges Kafka anomaly alerts to connected dashboard clients.
 * Consumes from the anomalies topic and broadcasts to all connected WebSocket sessions.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Component
public class AlertWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AlertWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        LOG.info("WebSocket client connected: sessionId={}, totalClients={}", sessionId, sessions.size());

        Mono<Void> welcome = session.send(Flux.just(session.textMessage("{\"type\":\"CONNECTED\",\"message\":\"Subscribed to anomaly alerts\"}")));

        Mono<Void> input = session.receive()
                .doOnNext(message -> {
                    String payload = message.getPayloadAsText();
                    LOG.debug("Received from client: sessionId={}, payload={}", sessionId, payload);
                    if ("ping".equalsIgnoreCase(payload)) {
                        session.send(Flux.just(session.textMessage("pong"))).subscribe();
                    }
                })
                .doOnTerminate(() -> {
                    sessions.remove(sessionId);
                    LOG.info("WebSocket client disconnected: sessionId={}, remaining={}", sessionId, sessions.size());
                })
                .doOnError(e -> {
                    LOG.error("WebSocket error: sessionId={}", sessionId, e);
                    sessions.remove(sessionId);
                })
                .then();

        return Mono.zip(welcome, input).then();
    }

    @KafkaListener(
            topics = "${autosre.kafka.topics.anomalies:autosre.alerts.anomalies}",
            groupId = "api-gateway-ws-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAlert(String message) {
        LOG.debug("Received alert from Kafka, broadcasting to {} clients", sessions.size());
        broadcast(message);
    }

    private void broadcast(String message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.send(Flux.just(session.textMessage(message)))
                            .doOnError(e -> {
                                LOG.error("Failed to send to session: sessionId={}", session.getId(), e);
                                sessions.remove(session.getId());
                            })
                            .subscribe();
                }
            } catch (Exception e) {
                LOG.error("Failed to broadcast: sessionId={}", session.getId(), e);
                sessions.remove(session.getId());
            }
        });
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}