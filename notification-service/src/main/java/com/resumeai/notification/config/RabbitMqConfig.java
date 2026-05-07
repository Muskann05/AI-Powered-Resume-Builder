package com.resumeai.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXPORT_EXCHANGE = "export.exchange";
    public static final String EXPORT_READY_QUEUE = "export.ready.queue";
    public static final String EXPORT_READY_ROUTING_KEY = "export.ready";

    public static final String RESUMEAI_EVENTS_EXCHANGE = "resumeai.events";
    public static final String PLAN_CHANGED_QUEUE = "plan.changed.queue";
    public static final String PLAN_CHANGED_ROUTING_KEY = "plan.changed";

    @Bean
    public Queue exportReadyQueue() {
        return new Queue(EXPORT_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange exportExchange() {
        return new DirectExchange(EXPORT_EXCHANGE);
    }

    @Bean
    public Binding exportReadyBinding() {
        return BindingBuilder.bind(exportReadyQueue())
                .to(exportExchange())
                .with(EXPORT_READY_ROUTING_KEY);
    }

    @Bean
    public Queue planChangedQueue() {
        return new Queue(PLAN_CHANGED_QUEUE, true);
    }

    @Bean
    public DirectExchange resumeAiEventsExchange() {
        return new DirectExchange(RESUMEAI_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding planChangedBinding() {
        return BindingBuilder.bind(planChangedQueue())
                .to(resumeAiEventsExchange())
                .with(PLAN_CHANGED_ROUTING_KEY);
    }
}
