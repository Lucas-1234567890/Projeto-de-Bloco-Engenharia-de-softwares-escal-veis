package com.lucas.todo.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia do lado produtor.
 * <p>
 * O todo-api só conhece o <b>exchange</b> — nunca uma fila. Quem quiser
 * receber eventos declara a própria fila e a liga ao exchange; o produtor
 * não precisa mudar nem saber quem são os consumidores.
 */
@Configuration
public class RabbitConfig {

    /** Exchange do tipo topic: roteia por padrão de routing key (task.created, task.completed...). */
    public static final String EXCHANGE = "task.events";

    /** Prefixo das routing keys: {@code task.<eventType em minúsculo>}. */
    public static final String ROUTING_KEY_PREFIX = "task.";

    @Bean
    public TopicExchange taskEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false); // durável, sem auto-delete
    }

    /** Serializa as mensagens como JSON (em vez de serialização Java nativa). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
