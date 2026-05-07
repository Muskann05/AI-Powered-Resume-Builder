package com.resumeai.export.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXPORT_QUEUE = "export.queue";
    public static final String EXPORT_READY_QUEUE = "export.ready.queue";
    public static final String EXPORT_EXCHANGE = "export.exchange";
    public static final String EXPORT_ROUTING_KEY = "export.routing";
    public static final String EXPORT_READY_ROUTING_KEY = "export.ready";

    @Bean
    public Queue exportQueue() {
        return new Queue(EXPORT_QUEUE, true);
    }

    @Bean
    public Queue exportReadyQueue() {
        return new Queue(EXPORT_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange exportExchange() {
        return new DirectExchange(EXPORT_EXCHANGE);
    }

    @Bean
    public Binding exportBinding() {
        return BindingBuilder.bind(exportQueue())
                .to(exportExchange())
                .with(EXPORT_ROUTING_KEY);
    }

    @Bean
    public Binding exportReadyBinding() {
        return BindingBuilder.bind(exportReadyQueue())
                .to(exportExchange())
                .with(EXPORT_READY_ROUTING_KEY);
    }
}
