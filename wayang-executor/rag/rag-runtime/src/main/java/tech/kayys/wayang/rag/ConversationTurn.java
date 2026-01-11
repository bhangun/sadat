package tech.kayys.silat.executor.rag.examples;

import java.time.Instant;

record ConversationTurn(
    String userMessage,
    String assistantMessage,
    Instant timestamp
) {}