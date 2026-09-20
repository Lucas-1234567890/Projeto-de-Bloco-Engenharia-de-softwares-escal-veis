package com.lucas.history.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do lado consumidor. Quem consome é dono da própria fila:
 *
 * <pre>
 *  task.events (topic) --[task.*]--> history.task-events --(rejeitada após retries)--> task.events.dlx
 *                                                                                          |
 *                                                                          history.task-events.dlq
 * </pre>
 *
 * A declaração é idempotente: se o exchange já foi declarado pelo todo-api
 * com os mesmos parâmetros, o RabbitMQ simplesmente reaproveita.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "task.events";
    public static final String HISTORY_QUEUE = "history.task-events";

    /** Dead Letter Exchange: para onde vão as mensagens que o consumidor não conseguiu processar. */
    public static final String DEAD_LETTER_EXCHANGE = "task.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "history.task-events.dlq";

    /** O histórico registra TODOS os eventos de task: created, updated, completed, deleted. */
    public static final String HISTORY_BINDING_KEY = "task.*";

    @Bean
    public TopicExchange taskEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue historyQueue() {
        return QueueBuilder.durable(HISTORY_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue historyDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding historyBinding() {
        return BindingBuilder.bind(historyQueue()).to(taskEventsExchange()).with(HISTORY_BINDING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(historyDeadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
