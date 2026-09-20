package com.lucas.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskCompletedMessageTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deveLerSoOsCamposNecessariosDoEventoCompleto() throws Exception {
        // JSON completo publicado pelo todo-api; os campos extras devem ser ignorados.
        String json = """
                {"eventId":"6c1c1f0e-8a52-4a53-a1b5-0f3c8a1f7a11","eventType":"COMPLETED","taskId":7,
                 "titulo":"Entregar TP4","descricao":"x","completed":true,
                 "occurredAt":"2026-09-20T10:30:00"}
                """;

        TaskCompletedMessage msg = mapper.readValue(json, TaskCompletedMessage.class);

        assertThat(msg.taskId()).isEqualTo(7L);
        assertThat(msg.titulo()).isEqualTo("Entregar TP4");
        assertThat(msg.occurredAt()).isNotNull();
    }
}
