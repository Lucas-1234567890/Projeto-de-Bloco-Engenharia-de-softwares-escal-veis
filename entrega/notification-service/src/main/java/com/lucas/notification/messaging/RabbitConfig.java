package com.lucas.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fila própria deste serviço, ligada SÓ à routing key {@code task.completed}.
 * Enquanto o history-service usa {@code task.*} (recebe tudo), este recebe
 * apenas conclusões — é o roteamento por tópico em ação.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "task.events";
    public static final String QUEUE = "notification.task-completed";
    public static final String BINDING_KEY = "task.completed";

    @Bean
    public TopicExchange taskEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(taskEventsExchange()).with(BINDING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
