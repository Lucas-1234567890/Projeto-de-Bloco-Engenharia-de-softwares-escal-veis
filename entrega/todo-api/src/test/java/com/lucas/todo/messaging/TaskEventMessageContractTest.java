package com.lucas.todo.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava o contrato JSON do evento. Se alguém renomear um campo aqui, este
 * teste quebra — e o history-service / notification-service, que leem esse
 * JSON, quebrariam em produção sem nenhum erro de compilação avisando.
 */
class TaskEventMessageContractTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void deveSerializarComOsCamposDoContrato() throws Exception {
        UUID id = UUID.randomUUID();
        TaskEventMessage evento = new TaskEventMessage(
                id, "COMPLETED", 42L, "Estudar RabbitMQ", "TP4", true,
                LocalDateTime.of(2026, 9, 20, 10, 30, 0));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(evento));

        assertThat(json.get("eventId").asText()).isEqualTo(id.toString());
        assertThat(json.get("eventType").asText()).isEqualTo("COMPLETED");
        assertThat(json.get("taskId").asLong()).isEqualTo(42L);
        assertThat(json.get("titulo").asText()).isEqualTo("Estudar RabbitMQ");
        assertThat(json.get("descricao").asText()).isEqualTo("TP4");
        assertThat(json.get("completed").asBoolean()).isTrue();
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-09-20T10:30:00");
    }
}
