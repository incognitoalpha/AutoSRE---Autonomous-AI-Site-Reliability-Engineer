package com.autosre.agent.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RunbookRetrievalService.RunbookChunk")
class RunbookChunkTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("accepts valid chunk")
        void acceptsValidChunk() {
            RunbookRetrievalService.RunbookChunk chunk = new RunbookRetrievalService.RunbookChunk(
                    "Kafka Memory Saturation",
                    "When Kafka brokers run out of memory...",
                    0.85,
                    "Troubleshooting"
            );
            assertNotNull(chunk);
            assertEquals("Kafka Memory Saturation", chunk.title());
            assertEquals(0.85, chunk.similarityScore());
        }

        @Test
        @DisplayName("rejects blank title")
        void rejectsBlankTitle() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RunbookRetrievalService.RunbookChunk("", "content", 0.5, "section")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RunbookRetrievalService.RunbookChunk("   ", "content", 0.5, "section")
            );
        }

        @Test
        @DisplayName("rejects blank chunk content")
        void rejectsBlankContent() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RunbookRetrievalService.RunbookChunk("Title", "", 0.5, "section")
            );
        }

        @Test
        @DisplayName("rejects similarity score below 0.0")
        void rejectsScoreBelowZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RunbookRetrievalService.RunbookChunk("Title", "content", -0.1, "section")
            );
        }

        @Test
        @DisplayName("rejects similarity score above 1.0")
        void rejectsScoreAboveOne() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RunbookRetrievalService.RunbookChunk("Title", "content", 1.5, "section")
            );
        }
    }

    @Nested
    @DisplayName("toPromptFragment")
    class ToPromptFragment {

        @Test
        @DisplayName("formats chunk for prompt injection")
        void formatsChunkForPrompt() {
            RunbookRetrievalService.RunbookChunk chunk = new RunbookRetrievalService.RunbookChunk(
                    "Pod CrashLoop",
                    "When pods are in CrashLoopBackOff state...",
                    0.92,
                    "Resolution Steps"
            );
            String fragment = chunk.toPromptFragment();
            assertTrue(fragment.contains("Pod CrashLoop"));
            assertTrue(fragment.contains("Resolution Steps"));
            assertTrue(fragment.contains("0.92"));
        }

        @Test
        @DisplayName("handles null section gracefully")
        void handlesNullSection() {
            RunbookRetrievalService.RunbookChunk chunk = new RunbookRetrievalService.RunbookChunk(
                    "Generic Runbook",
                    "General troubleshooting steps...",
                    0.75,
                    null
            );
            String fragment = chunk.toPromptFragment();
            assertTrue(fragment.contains("Generic Runbook"));
            assertTrue(fragment.contains("General"));
        }
    }
}