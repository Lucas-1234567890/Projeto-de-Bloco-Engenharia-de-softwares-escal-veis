package com.lucas.history.messaging;

import com.lucas.history.service.TaskHistoryService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor dos eventos de task.
 * <p>
 * Se {@code registrar} lançar exceção, o Spring AMQP tenta novamente (retry
 * com backoff exponencial, configurado no application.properties). Esgotadas
 * as tentativas, a mensagem é rejeitada sem requeue e o RabbitMQ a envia para
 * a Dead Letter Queue — assim uma "poison message" não trava a fila inteira.
 */
@Component
public class TaskEventListener {

    private final TaskHistoryService service;

    public TaskEventListener(TaskHistoryService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.HISTORY_QUEUE)
    public void aoReceberEvento(TaskEventMessage event) {
        service.registrar(event);
    }
}
