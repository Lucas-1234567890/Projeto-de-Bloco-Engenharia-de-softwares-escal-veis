package com.lucas.todo.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica no RabbitMQ os eventos emitidos pelo {@code TaskService}.
 * <p>
 * <b>Por que AFTER_COMMIT?</b> Se a mensagem fosse enviada dentro da
 * transação e o banco desse rollback depois, os consumidores receberiam um
 * evento sobre algo que nunca aconteceu. Publicando só após o commit, o
 * evento sempre corresponde a um estado realmente persistido.
 * <p>
 * <b>Limitação conhecida (dual write):</b> se o broker estiver fora do ar
 * exatamente depois do commit, o evento é perdido (apenas logado). A solução
 * completa é o padrão Transactional Outbox — ver docs/tp4-arquitetura-orientada-a-eventos.md.
 */
@Component
public class TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TaskEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TaskEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicar(TaskEventMessage event) {
        String routingKey = RabbitConfig.ROUTING_KEY_PREFIX + event.eventType().toLowerCase();
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
            log.info("Evento publicado: {} (taskId={}, eventId={})",
                    routingKey, event.taskId(), event.eventId());
        } catch (AmqpException ex) {
            // O CRUD da task já foi commitado e não deve falhar por causa da mensageria.
            log.error("Falha ao publicar evento {} (taskId={}, eventId={}): {}",
                    routingKey, event.taskId(), event.eventId(), ex.getMessage());
        }
    }
}
