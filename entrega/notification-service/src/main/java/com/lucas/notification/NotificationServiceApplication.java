package com.lucas.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Serviço que reage a tarefas concluídas. Existe para demonstrar o principal
 * ganho da arquitetura orientada a eventos: ele foi ADICIONADO ao sistema sem
 * nenhuma alteração no todo-api — bastou declarar uma fila e ligá-la ao
 * exchange {@code task.events}.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
