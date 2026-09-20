package com.lucas.history.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato do evento consumido do exchange {@code task.events}.
 * <p>
 * Repare que é uma classe <b>própria</b> deste serviço — não há biblioteca
 * compartilhada com o todo-api. O único acoplamento entre os dois é o JSON
 * (contrato). {@code ignoreUnknown = true} é o padrão <i>Tolerant Reader</i>:
 * se o produtor adicionar um campo novo, este consumidor continua funcionando.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskEventMessage(
        UUID eventId,
        String eventType,
        Long taskId,
        String titulo,
        String descricao,
        boolean completed,
        LocalDateTime occurredAt
) {
}
