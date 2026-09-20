package com.lucas.history.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lado consumidor do contrato: o JSON abaixo é exatamente o que o todo-api
 * publica (ver TaskEventMessageContractTest no todo-api). Os dois testes
 * juntos garantem que produtor e consumidor concordam sobre o formato,
 * mesmo sem compartilhar nenhuma classe.
 */
class TaskEventMessageContractTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deveLerOJsonPublicadoPeloTodoApi() throws Exception {
        UUID id = UUID.randomUUID();
        String json = """
                {"eventId":"%s","eventType":"COMPLETED","taskId":42,
                 "titulo":"Estudar RabbitMQ","descricao":"TP4","completed":true,
                 "occurredAt":"2026-09-20T10:30:00"}
                """.formatted(id);

        TaskEventMessage evento = mapper.readValue(json, TaskEventMessage.class);

        assertThat(evento.eventId()).isEqualTo(id);
        assertThat(evento.eventType()).isEqualTo("COMPLETED");
        assertThat(evento.taskId()).isEqualTo(42L);
        assertThat(evento.completed()).isTrue();
        assertThat(evento.occurredAt()).isEqualTo(LocalDateTime.of(2026, 9, 20, 10, 30, 0));
    }

    @Test
    void deveIgnorarCamposNovosAdicionadosPeloProdutor() throws Exception {
        // Tolerant Reader: o produtor pode evoluir o evento sem quebrar este consumidor.
        String json = """
                {"eventId":"%s","eventType":"CREATED","taskId":1,"titulo":"x","descricao":null,
                 "completed":false,"occurredAt":"2026-09-20T10:30:00","prioridade":"ALTA"}
                """.formatted(UUID.randomUUID());

        TaskEventMessage evento = mapper.readValue(json, TaskEventMessage.class);

        assertThat(evento.eventType()).isEqualTo("CREATED");
    }
}
