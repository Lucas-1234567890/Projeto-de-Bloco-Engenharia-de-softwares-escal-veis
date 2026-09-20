package com.lucas.todo.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TaskEventPublisher(rabbitTemplate);
    }

    private TaskEventMessage evento(String tipo) {
        return new TaskEventMessage(UUID.randomUUID(), tipo, 1L, "Tarefa", "desc", false, LocalDateTime.now());
    }

    @Test
    void deveEnviarParaOExchangeComRoutingKeyDerivadaDoTipo() {
        TaskEventMessage evento = evento("CREATED");

        publisher.publicar(evento);

        verify(rabbitTemplate).convertAndSend(eq("task.events"), eq("task.created"), eq(evento));
    }

    @Test
    void routingKeyDeConclusaoDeveSerTaskCompleted() {
        TaskEventMessage evento = evento("COMPLETED");

        publisher.publicar(evento);

        verify(rabbitTemplate).convertAndSend(eq("task.events"), eq("task.completed"), eq(evento));
    }

    @Test
    void naoDevePropagarExcecaoQuandoBrokerIndisponivel() {
        // O commit da task já aconteceu: falha de mensageria não pode virar erro 500 para o usuário.
        doThrow(new AmqpException("broker fora do ar"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publicar(evento("UPDATED"))).doesNotThrowAnyException();
    }
}
