package com.resumeai.auth.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String RESUMEAI_EVENTS_EXCHANGE = "resumeai.events";
    public static final String PLAN_CHANGED_ROUTING_KEY = "plan.changed";

    @Bean
    public Exchange resumeAiEventsExchange() {
        return new DirectExchange(RESUMEAI_EVENTS_EXCHANGE, true, false);
    }
}
