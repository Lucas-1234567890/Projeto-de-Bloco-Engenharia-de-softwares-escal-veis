package com.lucas.todo.messaging;

import com.lucas.todo.model.Task;
import com.lucas.todo.model.TaskAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio publicado no RabbitMQ sempre que uma task muda de estado.
 * <p>
 * Segue o padrão <b>Event-Carried State Transfer</b>: a mensagem carrega o
 * snapshot da task no momento do evento, então quem consome não precisa
 * chamar o todo-api de volta para saber "o que aconteceu".
 * <ul>
 *   <li>{@code eventId}: identifica o evento de forma única — permite que o
 *       consumidor seja idempotente (o RabbitMQ garante entrega
 *       <i>at-least-once</i>, então duplicatas são possíveis).</li>
 *   <li>{@code occurredAt}: quando o fato aconteceu no produtor. O consumidor
 *       usa este valor (e não o horário em que a mensagem chegou), senão a
 *       latência da fila distorceria as métricas de histórico.</li>
 * </ul>
 * Este record também é o evento interno do Spring: o {@link TaskEventPublisher}
 * só o envia ao broker depois do COMMIT da transação.
 */
public record TaskEventMessage(
        UUID eventId,
        String eventType,
        Long taskId,
        String titulo,
        String descricao,
        boolean completed,
        LocalDateTime occurredAt
) {

    public static TaskEventMessage of(TaskAction action, Task task) {
        return new TaskEventMessage(
                UUID.randomUUID(),
                action.name(),
                task.getId(),
                task.getTitulo(),
                task.getDescricao(),
                task.isCompleted(),
                LocalDateTime.now()
        );
    }
}
