package com.resumeai.auth.service;

import com.resumeai.auth.messaging.PlanChangedEvent;
import com.resumeai.auth.messaging.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AuthEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPlanChanged(PlanChangedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.RESUMEAI_EVENTS_EXCHANGE,
                RabbitMqConfig.PLAN_CHANGED_ROUTING_KEY,
                event
        );
    }
}
