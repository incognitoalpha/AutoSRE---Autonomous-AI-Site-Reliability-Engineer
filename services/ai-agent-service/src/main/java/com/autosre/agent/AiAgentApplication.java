package com.autosre.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoSRE AI Agent Service - LangChain4j powered root cause analysis and multi-agent orchestration.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@SpringBootApplication
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}