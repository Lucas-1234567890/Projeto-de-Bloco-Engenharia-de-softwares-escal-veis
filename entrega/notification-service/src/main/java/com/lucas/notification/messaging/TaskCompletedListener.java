package com.lucas.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Reage à conclusão de uma tarefa. Aqui só registra em log; num sistema real
 * seria o ponto de enviar e-mail, push, mensagem no Slack etc.
 */
@Component
public class TaskCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(TaskCompletedListener.class);

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void aoConcluirTarefa(TaskCompletedMessage event) {
        log.info("NOTIFICAÇÃO: tarefa #{} \"{}\" foi concluída em {}",
                event.taskId(), event.titulo(), event.occurredAt());
    }
}
