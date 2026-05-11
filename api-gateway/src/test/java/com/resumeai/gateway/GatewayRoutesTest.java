package com.resumeai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.test.StepVerifier;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "service.auth.uri=http://localhost:8081",
        "service.resume.uri=http://localhost:8082",
        "service.section.uri=http://localhost:8083",
        "service.ai.uri=http://localhost:8084",
        "service.template.uri=http://localhost:8085",
        "service.export.uri=http://localhost:8086",
        "service.notification.uri=http://localhost:8088",
        "service.jobmatch.uri=http://localhost:8089"
})
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldLoadAllConfiguredRoutes() {
        StepVerifier.create(routeLocator.getRoutes().map(route -> route.getId()).collectList())
                .expectNextMatches(routeIds ->
                        routeIds.contains("auth-service")
                                && routeIds.contains("resume-service")
                                && routeIds.contains("section-service")
                                && routeIds.contains("ai-service")
                                && routeIds.contains("template-service")
                                && routeIds.contains("export-service")
                                && routeIds.contains("notification-service")
                                && routeIds.contains("jobmatch-service")
                )
                .verifyComplete();
    }
}
