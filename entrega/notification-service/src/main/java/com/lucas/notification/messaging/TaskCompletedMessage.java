package com.lucas.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Visão mínima do evento: este serviço só declara os campos de que precisa
 * (Tolerant Reader). O evento completo tem mais campos, que são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskCompletedMessage(
        Long taskId,
        String titulo,
        LocalDateTime occurredAt
) {
}
