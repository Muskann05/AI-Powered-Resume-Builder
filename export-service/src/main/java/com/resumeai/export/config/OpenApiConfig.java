package com.resumeai.export.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResumeAI Export Service API")
                        .version("1.0")
                        .description("Async export service with RabbitMQ and local storage"));
    }
}
